import base64
import datetime
import json
import re
from flask import request

struct = wiz.model("struct")


def _now_iso():
    return datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")


def _body():
    data = {}
    if request.is_json:
        parsed = request.get_json(silent=True)
        if isinstance(parsed, dict):
            data.update(parsed)
    data.update(request.form.to_dict())
    data.update(request.args.to_dict())
    return data


def _value(key, default=""):
    return _body().get(key, default)


def _bearer_token():
    auth = request.headers.get("Authorization", "")
    if auth.lower().startswith("bearer "):
        return auth.split(" ", 1)[1].strip()
    return _value("access_token", "")


def _require_device():
    token = _bearer_token()
    device = struct.mobile_device.authenticate_access(token)
    if device is None:
        wiz.response.status(401, message="모바일 인증이 필요합니다.")
    user = struct.user.get(id=device.get("user_id"))
    if user is None or user.get("status") != "active":
        wiz.response.status(401, message="이용할 수 없는 계정입니다.")
    return device, user


def _login():
    data = _body()
    email = str(data.get("email", "")).strip().lower()
    password = str(data.get("password", ""))
    if not email or not password:
        wiz.response.status(400, message="이메일과 비밀번호를 입력해주세요.")

    user, message = struct.user.authenticate(email, password)
    if user is None:
        wiz.response.status(401, message=message)

    device, access_token, refresh_token = struct.mobile_device.create_session(
        user.get("id"),
        device_name=data.get("device_name", ""),
        platform=data.get("platform", "android"),
        ip=wiz.request.ip(),
        user_agent=request.headers.get("User-Agent", ""),
    )
    try:
        struct.access_log.record(user.get("id"), action="mobile_login", ip=wiz.request.ip(), user_agent=request.headers.get("User-Agent", ""))
    except Exception:
        pass
    wiz.response.status(200, user=user, device=device, access_token=access_token, refresh_token=refresh_token, server_time=_now_iso())


def _refresh():
    result, message = struct.mobile_device.refresh(_value("refresh_token", ""))
    if result is None:
        wiz.response.status(401, message=message)
    wiz.response.status(200, **result, server_time=_now_iso())


def _logout():
    token = _bearer_token()
    if token:
        struct.mobile_device.revoke_access(token)
    wiz.response.status(200, ok=True)


def _sync_cards():
    device, user = _require_device()
    cards = struct.card.mobile_sync(user.get("id"), since=_value("since", ""))
    wiz.response.status(200, cards=cards, count=len(cards), server_time=_now_iso(), device=device)


def _overlay_image(card_id):
    _device, user = _require_device()
    kind = _value("kind", "generated")
    image = struct.card.mobile_overlay_image(user.get("id"), card_id, kind=kind)
    if image is None:
        wiz.response.status(404, message="명함 이미지를 찾을 수 없습니다.")
    headers = {
        "Cache-Control": "private, max-age=31536000, immutable",
        "X-Image-Hash": image.get("hash", ""),
        "Content-Disposition": f"inline; filename=\"{image.get('filename', 'business-card.png')}\"",
    }
    try:
        wiz.response.headers(headers)
    except Exception:
        pass
    wiz.response.send(image.get("data", b""), content_type=image.get("mime", "application/octet-stream"))


def _overlay_images_batch():
    _device, user = _require_device()
    data = _body()
    items = data.get("items", [])
    if isinstance(items, str):
        try:
            items = json.loads(items or "[]")
        except Exception:
            wiz.response.status(400, message="items JSON이 올바르지 않습니다.")
    if not isinstance(items, list):
        wiz.response.status(400, message="items는 배열이어야 합니다.")
    if len(items) > 500:
        wiz.response.status(400, message="한 번에 최대 500개 이미지까지 동기화할 수 있습니다.")

    images = []
    for item in items:
        if not isinstance(item, dict):
            continue
        card_id = str(item.get("card_id", "") or "").strip()
        kind = str(item.get("kind", "generated") or "generated").strip()
        if not card_id:
            continue
        image = struct.card.mobile_overlay_image(user.get("id"), card_id, kind=kind)
        if image is None:
            images.append(dict(card_id=card_id, requested_kind=kind, missing=True))
            continue
        images.append(dict(
            card_id=card_id,
            requested_kind=kind,
            kind=image.get("kind", kind),
            mime=image.get("mime", "application/octet-stream"),
            hash=image.get("hash", ""),
            filename=image.get("filename", "business-card.png"),
            data=base64.b64encode(image.get("data", b"")).decode("ascii"),
        ))
    wiz.response.status(200, images=images, count=len(images), server_time=_now_iso())


def _save_overlay_settings(device_id):
    device, user = _require_device()
    if device_id != device.get("id"):
        wiz.response.status(403, message="다른 기기 설정은 수정할 수 없습니다.")
    data = _body()
    settings = data.get("settings", {})
    if isinstance(settings, str):
        try:
            settings = json.loads(settings or "{}")
        except Exception:
            wiz.response.status(400, message="settings JSON이 올바르지 않습니다.")
    saved = struct.mobile_device.save_overlay_settings(device_id, user.get("id"), settings)
    if saved is None:
        wiz.response.status(404, message="기기를 찾을 수 없습니다.")
    wiz.response.status(200, device=saved)


def _health():
    wiz.response.status(200, ok=True, server_time=_now_iso())


segment = wiz.request.match("/api/mobile/<path:action>")
if segment is None:
    wiz.response.status(404)

action = str(getattr(segment, "action", "") or "").strip("/")
method = request.method.upper()

if action == "health":
    _health()
elif action == "auth/login" and method == "POST":
    _login()
elif action == "auth/refresh" and method == "POST":
    _refresh()
elif action == "auth/logout" and method == "POST":
    _logout()
elif action == "cards/sync" and method == "GET":
    _sync_cards()
elif action == "cards/overlay-images" and method == "POST":
    _overlay_images_batch()
else:
    image_match = re.match(r"^cards/([^/]+)/overlay-image$", action)
    settings_match = re.match(r"^devices/([^/]+)/overlay-settings$", action)
    if image_match and method == "GET":
        _overlay_image(image_match.group(1))
    elif settings_match and method == "POST":
        _save_overlay_settings(settings_match.group(1))
    else:
        wiz.response.status(404)
