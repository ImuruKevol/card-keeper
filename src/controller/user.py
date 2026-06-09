import season


class Controller(wiz.controller("base")):
    def __init__(self):
        super().__init__()

        if wiz.session.has("id") is False:
            wiz.response.status(401)

        struct = wiz.model("struct")
        user_id = wiz.session.get("id")
        session_token = wiz.session.get("session_token", "")

        try:
            revoked = struct.user_session.is_revoked(session_token) if session_token else False
            user = struct.user.get(id=user_id)
        except Exception:
            wiz.session.clear()
            wiz.response.status(401)

        if revoked:
            wiz.session.clear()
            wiz.response.status(401)

        if user is None or user.get("status") != "active":
            wiz.session.clear()
            wiz.response.status(401)

        try:
            struct.user.touch(user_id)
            if session_token:
                struct.user_session.update_active(session_token)
        except Exception:
            pass
