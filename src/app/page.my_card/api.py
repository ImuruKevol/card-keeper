struct = wiz.model("struct")

FIELDS = [
    "name", "company", "department", "position", "email", "mobile", "phone",
    "address", "website", "tagline", "main_color", "accent_color", "theme", "card_image",
]


def _current_user_id():
    user_id = wiz.session.get("id", "")
    if not user_id:
        wiz.response.status(401, message="로그인이 필요합니다.")
    return user_id


def _payload():
    data = {}
    for key in FIELDS:
        data[key] = wiz.request.query(key, "")
    return data


def load():
    user_id = _current_user_id()
    card = struct.my_card.get_or_default(user_id)
    wiz.response.status(200, card=card)


def save():
    user_id = _current_user_id()
    try:
        card = struct.my_card.save(user_id, _payload())
    except Exception as e:
        wiz.response.status(400, message=str(e))
    wiz.response.status(200, card=card)


def publish():
    user_id = _current_user_id()
    try:
        card = struct.my_card.publish(user_id, _payload())
    except Exception as e:
        wiz.response.status(400, message=str(e))
    wiz.response.status(200, card=card)


def unpublish():
    user_id = _current_user_id()
    try:
        card = struct.my_card.unpublish(user_id)
    except Exception as e:
        wiz.response.status(400, message=str(e))
    wiz.response.status(200, card=card)
