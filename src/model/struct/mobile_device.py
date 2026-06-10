import datetime
import hashlib
import json
import secrets


class MobileDevice:
    ACCESS_TOKEN_MINUTES = 60
    REFRESH_TOKEN_DAYS = 45

    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("mobile_device")

    def _now(self):
        return datetime.datetime.utcnow()

    def _iso(self, value):
        if not value:
            return ""
        if isinstance(value, str):
            return value
        return value.strftime("%Y-%m-%dT%H:%M:%SZ")

    def _hash(self, token):
        return hashlib.sha256(str(token or "").encode("utf-8")).hexdigest()

    def _token(self):
        return secrets.token_urlsafe(48)

    def _text(self, value, limit):
        return str(value or "").strip()[:limit]

    def _public(self, row):
        if row is None:
            return None
        item = dict(row)
        item.pop("access_token_hash", None)
        item.pop("refresh_token_hash", None)
        for key in ["created", "updated", "last_seen", "access_expires", "refresh_expires"]:
            item[key] = self._iso(item.get(key))
        try:
            item["overlay_settings"] = json.loads(item.get("overlay_settings") or "{}")
        except Exception:
            item["overlay_settings"] = {}
        return item

    def create_session(self, user_id, device_name="", platform="android", ip="", user_agent=""):
        access_token = self._token()
        refresh_token = self._token()
        now = self._now()
        access_expires = now + datetime.timedelta(minutes=self.ACCESS_TOKEN_MINUTES)
        refresh_expires = now + datetime.timedelta(days=self.REFRESH_TOKEN_DAYS)
        device_id = self.db.insert(dict(
            user_id=user_id,
            device_name=self._text(device_name, 160),
            platform=self._text(platform or "android", 32),
            access_token_hash=self._hash(access_token),
            refresh_token_hash=self._hash(refresh_token),
            ip=self._text(ip, 64),
            user_agent=str(user_agent or "")[:1000],
            status="active",
            created=now,
            updated=now,
            last_seen=now,
            access_expires=access_expires,
            refresh_expires=refresh_expires,
        ))
        row = self.db.get(id=device_id)
        return self._public(row), access_token, refresh_token

    def _active_by(self, **where):
        row = self.db.get(status="active", **where)
        if row is None:
            return None
        now = self._now()
        if row.get("refresh_expires") and row.get("refresh_expires") < now:
            self.revoke(row.get("id"))
            return None
        return row

    def authenticate_access(self, access_token):
        if not access_token:
            return None
        row = self._active_by(access_token_hash=self._hash(access_token))
        if row is None:
            return None
        now = self._now()
        if row.get("access_expires") and row.get("access_expires") < now:
            return None
        self.db.update(dict(last_seen=now, updated=now), id=row.get("id"))
        return self._public(self.db.get(id=row.get("id")))

    def refresh(self, refresh_token):
        if not refresh_token:
            return None, "refresh token이 필요합니다."
        row = self._active_by(refresh_token_hash=self._hash(refresh_token))
        if row is None:
            return None, "유효하지 않은 refresh token입니다."

        access_token = self._token()
        refresh_token = self._token()
        now = self._now()
        data = dict(
            access_token_hash=self._hash(access_token),
            refresh_token_hash=self._hash(refresh_token),
            access_expires=now + datetime.timedelta(minutes=self.ACCESS_TOKEN_MINUTES),
            refresh_expires=now + datetime.timedelta(days=self.REFRESH_TOKEN_DAYS),
            last_seen=now,
            updated=now,
        )
        self.db.update(data, id=row.get("id"))
        device = self._public(self.db.get(id=row.get("id")))
        return dict(device=device, access_token=access_token, refresh_token=refresh_token), None

    def revoke(self, device_id):
        if not device_id:
            return
        try:
            self.db.update(dict(status="revoked", updated=self._now()), id=device_id)
        except Exception:
            pass

    def revoke_access(self, access_token):
        row = self.db.get(access_token_hash=self._hash(access_token)) if access_token else None
        if row is None:
            return False
        self.revoke(row.get("id"))
        return True

    def save_overlay_settings(self, device_id, user_id, settings):
        row = self.db.get(id=device_id, user_id=user_id, status="active")
        if row is None:
            return None
        try:
            payload = json.dumps(settings or {}, ensure_ascii=False)
        except Exception:
            payload = "{}"
        self.db.update(dict(overlay_settings=payload, updated=self._now()), id=device_id)
        return self._public(self.db.get(id=device_id))


Model = MobileDevice
