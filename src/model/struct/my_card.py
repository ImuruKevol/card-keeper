import datetime
import re
import secrets


class MyCard:
    FIELDS = [
        "name", "company", "department", "position", "email", "mobile", "phone",
        "address", "website", "tagline", "main_color", "accent_color", "theme", "card_image",
    ]
    THEMES = ["signature", "lattice", "flow"]
    COLOR_RE = re.compile(r"^#[0-9a-fA-F]{6}$")
    IMAGE_RE = re.compile(r"^data:image/(?:png|jpeg|jpg|webp);base64,[A-Za-z0-9+/=\r\n]+$")
    MAX_IMAGE_LENGTH = 5 * 1024 * 1024

    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("my_card")
        self._ensure_columns()

    def _ensure_columns(self):
        try:
            model = self.db.orm
            database = model._meta.database
            table = model._meta.table_name
            existing = [column.name for column in database.get_columns(table)]
            is_mysql = "mysql" in type(database).__name__.lower()
            quote = "`" if is_mysql else '"'
            specs = {
                "tagline": "VARCHAR(180) DEFAULT ''",
                "main_color": "VARCHAR(16) DEFAULT '#123c69'",
                "accent_color": "VARCHAR(16) DEFAULT '#14b8a6'",
                "theme": "VARCHAR(32) DEFAULT 'signature'",
                "card_image": "LONGTEXT NULL" if is_mysql else "TEXT DEFAULT ''",
                "share_token": "VARCHAR(64) NULL",
                "public_enabled": "TINYINT(1) DEFAULT 0" if is_mysql else "INTEGER DEFAULT 0",
            }
            for name, column_type in specs.items():
                if name not in existing:
                    database.execute_sql(f"ALTER TABLE {quote}{table}{quote} ADD COLUMN {quote}{name}{quote} {column_type}")
        except Exception:
            pass

    def _now(self):
        return datetime.datetime.now()

    def _text(self, value, max_length):
        value = "" if value is None else str(value)
        return value.strip()[:max_length]

    def _color(self, value, fallback):
        value = self._text(value, 16)
        if self.COLOR_RE.match(value):
            return value.lower()
        return fallback

    def _theme(self, value):
        value = self._text(value, 32)
        if value in self.THEMES:
            return value
        return "signature"

    def _image(self, value):
        value = "" if value is None else str(value).strip()
        if not value:
            return ""
        if len(value) > self.MAX_IMAGE_LENGTH:
            raise Exception("명함 이미지가 너무 큽니다.")
        if self.IMAGE_RE.match(value) is None:
            raise Exception("명함 이미지는 PNG, JPG, WEBP 데이터만 사용할 수 있습니다.")
        return value

    def _clean(self, data, existing=None):
        existing = existing or {}
        cleaned = {
            "name": self._text(data.get("name", existing.get("name", "")), 120),
            "company": self._text(data.get("company", existing.get("company", "")), 160),
            "department": self._text(data.get("department", existing.get("department", "")), 120),
            "position": self._text(data.get("position", existing.get("position", "")), 120),
            "email": self._text(data.get("email", existing.get("email", "")), 191),
            "mobile": self._text(data.get("mobile", existing.get("mobile", "")), 64),
            "phone": self._text(data.get("phone", existing.get("phone", "")), 64),
            "address": self._text(data.get("address", existing.get("address", "")), 255),
            "website": self._text(data.get("website", existing.get("website", "")), 255),
            "tagline": self._text(data.get("tagline", existing.get("tagline", "")), 180),
            "main_color": self._color(data.get("main_color", existing.get("main_color", "")), "#123c69"),
            "accent_color": self._color(data.get("accent_color", existing.get("accent_color", "")), "#14b8a6"),
            "theme": self._theme(data.get("theme", existing.get("theme", "signature"))),
            "card_image": self._image(data.get("card_image", existing.get("card_image", ""))),
        }
        if not cleaned["name"]:
            raise Exception("이름은 필수입니다.")
        return cleaned

    def _enabled(self, value):
        if isinstance(value, bool):
            return value
        return str(value).lower() in ["1", "true", "yes", "y"]

    def _serialize(self, row):
        if row is None:
            return None
        item = dict(row)
        item["public_enabled"] = self._enabled(item.get("public_enabled"))
        if item.get("share_token") is None:
            item["share_token"] = ""
        return item

    def _default(self, owner_id):
        user = None
        try:
            user = self.core.user.get(id=owner_id)
        except Exception:
            user = None
        user = user or {}
        return dict(
            id="",
            owner_id=owner_id,
            name=user.get("name", ""),
            company="",
            department="",
            position="",
            email=user.get("email", ""),
            mobile=user.get("mobile", ""),
            phone="",
            address="",
            website="",
            tagline="",
            main_color="#123c69",
            accent_color="#14b8a6",
            theme="signature",
            card_image="",
            share_token="",
            public_enabled=False,
            status="active",
        )

    def get(self, owner_id):
        row = self.db.get(owner_id=owner_id, status="active")
        return self._serialize(row)

    def get_or_default(self, owner_id):
        return self.get(owner_id) or self._default(owner_id)

    def save(self, owner_id, data):
        existing = self.get(owner_id)
        item = self._clean(data, existing=existing)
        item["updated"] = self._now()
        if existing:
            self.db.update(item, owner_id=owner_id)
            return self.get(owner_id)

        now = self._now()
        item.update(dict(owner_id=owner_id, public_enabled=False, status="active", created=now, updated=now))
        self.db.insert(item)
        return self.get(owner_id)

    def _new_token(self):
        token = secrets.token_urlsafe(18)
        while self.db.get(share_token=token) is not None:
            token = secrets.token_urlsafe(18)
        return token

    def publish(self, owner_id, data):
        card = self.save(owner_id, data)
        image = self._image(data.get("card_image", ""))
        if not image:
            raise Exception("공유할 명함 이미지가 필요합니다.")
        token = card.get("share_token") or self._new_token()
        self.db.update(
            dict(share_token=token, public_enabled=True, card_image=image, updated=self._now()),
            owner_id=owner_id,
        )
        return self.get(owner_id)

    def unpublish(self, owner_id):
        card = self.get(owner_id)
        if card is None:
            return self._default(owner_id)
        self.db.update(dict(public_enabled=False, updated=self._now()), owner_id=owner_id)
        return self.get(owner_id)

    def public_by_token(self, token):
        token = self._text(token, 64)
        if not token:
            return None
        row = self.db.get(share_token=token, status="active")
        item = self._serialize(row)
        if item is None or not item.get("public_enabled") or not item.get("card_image"):
            return None
        return item


Model = MyCard
