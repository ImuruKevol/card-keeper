import datetime
import uuid


class UserSession:
    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("user_session")

    def register(self, user_id, ip="", user_agent="", login_type="login"):
        token = str(uuid.uuid4())
        now = datetime.datetime.now()
        self.db.insert(dict(
            id=token,
            user_id=user_id,
            ip=ip or "",
            user_agent=user_agent or "",
            login_type=login_type or "login",
            created=now,
            last_active=now,
            is_active=1,
        ))
        return token

    def is_revoked(self, token):
        if not token:
            return False
        try:
            row = self.db.get(id=token)
        except Exception:
            return False
        if row is None:
            return True
        return int(row.get("is_active", 0)) != 1

    def update_active(self, token):
        if not token:
            return
        try:
            self.db.update(dict(last_active=datetime.datetime.now()), id=token)
        except Exception:
            pass

    def deactivate(self, token):
        if not token:
            return
        try:
            self.db.update(dict(is_active=0, last_active=datetime.datetime.now()), id=token)
        except Exception:
            pass

    def list_active(self, user_id):
        return self.db.rows(user_id=user_id, is_active=1, orderby="last_active", order="DESC")

    def revoke(self, token, user_id):
        row = self.db.get(id=token)
        if row is None or row.get("user_id") != user_id:
            return False
        self.deactivate(token)
        return True

    def revoke_all(self, user_id, except_token=None):
        rows = self.list_active(user_id)
        for row in rows:
            if except_token and row.get("id") == except_token:
                continue
            self.deactivate(row.get("id"))


Model = UserSession
