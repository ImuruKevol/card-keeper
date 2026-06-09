import datetime


class AccessLog:
    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("access_log")

    def record(self, user_id, action="login", ip="", user_agent=""):
        if not user_id:
            return
        self.db.insert(dict(
            user_id=user_id,
            action=action,
            ip=ip or "",
            user_agent=user_agent or "",
            created=datetime.datetime.now(),
        ))

    def recent(self, user_id, page=1, dump=20):
        rows = self.db.rows(user_id=user_id, page=page, dump=dump, orderby="created", order="DESC")
        total = self.db.count(user_id=user_id) or 0
        return rows, total


Model = AccessLog
