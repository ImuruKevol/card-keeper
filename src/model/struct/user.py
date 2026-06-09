import datetime
import bcrypt


class User:
    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("user")

    def _now(self):
        return datetime.datetime.now()

    def _hash_password(self, password):
        if isinstance(password, str):
            password = password.encode("utf-8")
        return bcrypt.hashpw(password, bcrypt.gensalt()).decode("utf-8")

    def _check_password(self, password, hashed):
        try:
            if isinstance(password, str):
                password = password.encode("utf-8")
            if isinstance(hashed, str):
                hashed = hashed.encode("utf-8")
            return bcrypt.checkpw(password, hashed)
        except Exception:
            return False

    def _public(self, user):
        if user is None:
            return None
        item = dict(user)
        item.pop("password", None)
        return item

    def find(self, key):
        if not key:
            return None
        user = self.db.get(id=key)
        if user is None:
            user = self.db.get(email=key)
        return user

    def get(self, id=None, email=None):
        user = None
        if id:
            user = self.db.get(id=id)
        elif email:
            user = self.db.get(email=email)
        return self._public(user)

    def authenticate(self, email, password):
        user = self.db.get(email=email)
        if user is None:
            return None, "이메일 또는 비밀번호가 올바르지 않습니다."
        if user.get("status") == "pending":
            return None, "관리자 승인 후 이용할 수 있습니다."
        if user.get("status") != "active":
            return None, "이용할 수 없는 계정입니다."
        if self._check_password(password, user.get("password", "")) is False:
            return None, "이메일 또는 비밀번호가 올바르지 않습니다."
        return self._public(user), None

    def signup(self, data):
        email = (data.get("email") or "").strip().lower()
        password = data.get("password") or ""
        name = (data.get("name") or "").strip()
        mobile = (data.get("mobile") or "").strip()

        if not email or not password or not name:
            raise Exception("이름, 이메일, 비밀번호를 모두 입력해주세요.")
        if self.db.get(email=email) is not None:
            raise Exception("이미 가입된 이메일입니다.")

        first_user = self.count() == 0
        now = self._now()
        user_id = self.db.insert(dict(
            email=email,
            password=self._hash_password(password),
            name=name,
            mobile=mobile,
            role="admin" if first_user else "user",
            status="active" if first_user else "pending",
            memo="initial administrator" if first_user else "",
            created=now,
            updated=now,
        ))
        return self.get(id=user_id), first_user

    def create(self, data):
        email = (data.get("email") or "").strip().lower()
        password = data.get("password") or ""
        name = (data.get("name") or "").strip()
        if not email or not password or not name:
            raise Exception("이름, 이메일, 비밀번호가 필요합니다.")
        if self.db.get(email=email) is not None:
            raise Exception("이미 등록된 사용자입니다.")

        now = self._now()
        return self.db.insert(dict(
            email=email,
            password=self._hash_password(password),
            name=name,
            mobile=(data.get("mobile") or "").strip(),
            role=data.get("role") or "user",
            status=data.get("status") or "pending",
            memo=data.get("memo") or "",
            created=now,
            updated=now,
        ))

    def search(self, text="", status="", role="", page=1, dump=50):
        where = {}
        if status and status != "all":
            where["status"] = status
        if role and role != "all":
            where["role"] = role

        def query(db, qs):
            keyword = (text or "").strip()
            if keyword:
                qs = qs.where(
                    db.name.contains(keyword)
                    | db.email.contains(keyword)
                    | db.mobile.contains(keyword)
                )
            return qs

        rows = self.db.rows(query=query, page=page, dump=dump, orderby="created", order="DESC", **where)
        total = self.db.count(query=query, **where) or 0
        return [self._public(row) for row in rows], total

    def list(self, text="", role="", status=""):
        rows, _total = self.search(text=text, role=role, status=status, page=None, dump=1000)
        return rows

    def update_profile(self, id, **fields):
        data = {}
        for key in ["name", "mobile", "memo"]:
            if key in fields:
                data[key] = fields[key]
        if not data:
            return
        data["updated"] = self._now()
        self.db.update(data, id=id)

    def change_password(self, id, current_password, new_password):
        user = self.db.get(id=id)
        if user is None:
            return False
        if self._check_password(current_password, user.get("password", "")) is False:
            return False
        self.db.update(dict(password=self._hash_password(new_password), updated=self._now()), id=id)
        return True

    def approve(self, id):
        self.db.update(dict(status="active", updated=self._now()), id=id)
        return self.get(id=id)

    def block(self, id):
        self.db.update(dict(status="blocked", updated=self._now()), id=id)
        return self.get(id=id)

    def activate(self, id):
        self.db.update(dict(status="active", updated=self._now()), id=id)
        return self.get(id=id)

    def initial_admin_id(self):
        rows = self.db.rows(memo="initial administrator", page=1, dump=1, orderby="created", order="ASC")
        if rows:
            return rows[0].get("id", "")

        rows = self.db.rows(role="admin", page=1, dump=1, orderby="created", order="ASC")
        if rows:
            return rows[0].get("id", "")
        return ""

    def active_admin_count(self, exclude_id=None):
        def query(db, qs):
            if exclude_id:
                qs = qs.where(db.id != exclude_id)
            return qs

        return self.db.count(query=query, role="admin", status="active") or 0

    def _is_initial_admin(self, user):
        return user is not None and user.get("id") == self.initial_admin_id()

    def set_role(self, id, role, current_user_id=None):
        if role not in ["admin", "user"]:
            raise Exception("허용되지 않은 권한입니다.")

        user = self.db.get(id=id)
        if user is None:
            raise Exception("사용자를 찾을 수 없습니다.")

        if role == "user" and user.get("role") == "admin":
            if current_user_id and id == current_user_id:
                raise Exception("본인 관리자 권한은 해제할 수 없습니다.")
            if self._is_initial_admin(user):
                raise Exception("초기 관리자는 사용자로 변경할 수 없습니다.")
            if user.get("status") == "active" and self.active_admin_count(exclude_id=id) < 1:
                raise Exception("남은 관리자가 없어 사용자로 변경할 수 없습니다.")

        self.db.update(dict(role=role, updated=self._now()), id=id)
        return self.get(id=id)

    def touch(self, id):
        self.db.update(dict(last_access=self._now()), id=id)

    def count(self, **kwargs):
        return self.db.count(**kwargs) or 0


Model = User
