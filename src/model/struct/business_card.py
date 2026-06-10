import base64
import datetime
import hashlib
import io
import re


class BusinessCard:
    FIELDS = [
        "name", "company", "department", "position", "email", "mobile", "phone",
        "address", "website", "front_image", "back_image", "tags", "memo", "source",
    ]
    IMPORT_FIELDS = [
        "name", "company", "department", "position", "email", "mobile", "phone",
        "address", "website", "tags", "memo", "source",
    ]
    PHONE_KINDS = ["mobile", "phone"]
    DATA_URL_RE = re.compile(r"^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$", re.S)

    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("business_card")
        self._ensure_image_columns()

    def _ensure_image_columns(self):
        try:
            model = self.db.orm
            database = model._meta.database
            table = model._meta.table_name
            existing = [column.name for column in database.get_columns(table)]
            is_mysql = "mysql" in type(database).__name__.lower()
            quote = "`" if is_mysql else '"'
            column_type = "LONGTEXT NULL" if is_mysql else "TEXT DEFAULT ''"
            for name in ["front_image", "back_image"]:
                if name not in existing:
                    database.execute_sql(f"ALTER TABLE {quote}{table}{quote} ADD COLUMN {quote}{name}{quote} {column_type}")
        except Exception:
            pass

    def _now(self):
        return datetime.datetime.now()

    def _clean(self, data):
        cleaned = {}
        for key in self.FIELDS:
            value = data.get(key, "")
            if value is None:
                value = ""
            cleaned[key] = str(value).strip()
        if not cleaned.get("name"):
            raise Exception("이름은 필수입니다.")
        if not cleaned.get("source"):
            cleaned["source"] = "manual"
        return cleaned

    def _allowed(self, row, owner_id):
        if row is None:
            return False
        if self.core.isAdmin():
            return True
        return row.get("owner_id") == owner_id

    def search(self, owner_id, text="", page=1, dump=10, scope="name_company", sort="name", direction="asc"):
        where = {"status": "active"}
        if not self.core.isAdmin():
            where["owner_id"] = owner_id

        search_fields = {
            "name_company": ["name", "company"],
            "name": ["name"],
            "company": ["company", "department"],
            "info": ["company", "department", "position", "email", "mobile", "phone", "address", "website", "memo", "tags"],
            "all": ["name", "company", "department", "position", "email", "mobile", "phone", "address", "website", "memo", "tags"],
        }
        fields = search_fields.get(scope, search_fields["name_company"])

        def query(db, qs):
            keyword = (text or "").strip()
            if keyword:
                condition = None
                for field_name in fields:
                    field = getattr(db, field_name)
                    expression = field.contains(keyword)
                    condition = expression if condition is None else condition | expression
                qs = qs.where(condition)
            return qs

        sort_fields = {
            "name": "name,company,updated",
            "company": "company,name,updated",
        }
        orderby = sort_fields.get(sort, sort_fields["name"])
        order = "ASC" if str(direction).lower() == "asc" else "DESC"
        rows = self.db.rows(query=query, page=page, dump=dump, orderby=orderby, order=order, **where)
        total = self.db.count(query=query, **where) or 0
        return rows, total

    def count(self, owner_id):
        where = {"status": "active"}
        if not self.core.isAdmin():
            where["owner_id"] = owner_id
        return self.db.count(**where) or 0

    def export_rows(self, owner_id, text="", scope="name_company", sort="name", direction="asc", limit=10000):
        rows, total = self.search(owner_id, text=text, scope=scope, sort=sort, direction=direction, page=1, dump=limit)
        return rows, total

    def get(self, id, owner_id):
        row = self.db.get(id=id, status="active")
        if self._allowed(row, owner_id) is False:
            return None
        return row

    def create(self, owner_id, data):
        item = self._clean(data)
        now = self._now()
        item.update(dict(owner_id=owner_id, status="active", created=now, updated=now))
        return self.db.insert(item)

    def update(self, id, owner_id, data):
        row = self.get(id, owner_id)
        if row is None:
            raise Exception("명함을 찾을 수 없습니다.")
        item = self._clean(data)
        item["updated"] = self._now()
        self.db.update(item, id=id)
        return id

    def delete(self, id, owner_id):
        row = self.get(id, owner_id)
        if row is None:
            raise Exception("명함을 찾을 수 없습니다.")
        self.db.update(dict(status="deleted", updated=self._now()), id=id)

    def _active_rows(self, owner_id, limit=10000):
        where = {"status": "active"}
        if not self.core.isAdmin():
            where["owner_id"] = owner_id
        return self.db.rows(page=1, dump=limit, orderby="updated", order="DESC", **where)

    def _normalized_key_value(self, value, numeric=False):
        value = str(value or "").strip().lower()
        if numeric:
            value = "".join([letter for letter in value if letter.isdigit()])
        return value

    def _normalized_name_value(self, value):
        return re.sub(r"\s+", "", str(value or "").strip().lower())

    def same_name_candidates(self, owner_id, name, exclude_id="", limit=5):
        target = self._normalized_name_value(name)
        if not target:
            return []

        rows = []
        for row in self._active_rows(owner_id):
            if exclude_id and row.get("id") == exclude_id:
                continue
            if self._normalized_name_value(row.get("name")) != target:
                continue
            rows.append(row)
            if len(rows) >= limit:
                break
        return rows

    def _duplicate_keys(self, row):
        keys = []
        email = self._normalized_key_value(row.get("email"))
        mobile = self._normalized_key_value(row.get("mobile"), numeric=True)
        phone = self._normalized_key_value(row.get("phone"), numeric=True)
        name = self._normalized_key_value(row.get("name"))
        company = self._normalized_key_value(row.get("company"))
        if email:
            keys.append(f"email:{email}")
        if mobile:
            keys.append(f"mobile:{mobile}")
        if phone:
            keys.append(f"phone:{phone}")
        if name and company:
            keys.append(f"identity:{name}:{company}")
        return keys

    def _duplicate_lookup(self, owner_id):
        lookup = {}
        for row in self._active_rows(owner_id):
            for key in self._duplicate_keys(row):
                if key not in lookup:
                    lookup[key] = row
        return lookup

    def _import_payload(self, row, existing=None):
        payload = {key: "" for key in self.FIELDS}
        if existing:
            for key in self.FIELDS:
                payload[key] = existing.get(key, "")
        for key in self.IMPORT_FIELDS:
            value = row.get(key, "")
            if value is None:
                value = ""
            payload[key] = str(value).strip()
        if not payload.get("source"):
            payload["source"] = "import"
        return payload

    def import_rows(self, owner_id, rows, duplicate="skip"):
        result = dict(created=0, updated=0, skipped=0, failed=0, total=len(rows), errors=[])
        lookup = self._duplicate_lookup(owner_id)
        duplicate = duplicate if duplicate in ["skip", "update", "create"] else "skip"

        for row in rows:
            row_number = row.get("_row", "")
            keys = self._duplicate_keys(row)
            existing = None
            for key in keys:
                if key in lookup:
                    existing = lookup[key]
                    break

            try:
                if existing and duplicate == "skip":
                    result["skipped"] += 1
                    continue

                if existing and duplicate == "update":
                    payload = self._import_payload(row, existing=existing)
                    self.update(existing["id"], owner_id, payload)
                    updated = self.get(existing["id"], owner_id)
                    for key in self._duplicate_keys(updated or payload):
                        lookup[key] = updated or existing
                    result["updated"] += 1
                    continue

                payload = self._import_payload(row)
                card_id = self.create(owner_id, payload)
                created = self.get(card_id, owner_id) or payload
                for key in self._duplicate_keys(created):
                    lookup[key] = created
                result["created"] += 1
            except Exception as e:
                result["failed"] += 1
                result["errors"].append(dict(row=row_number, message=str(e)))

        return result

    def recent(self, owner_id, dump=5):
        where = {"status": "active"}
        if not self.core.isAdmin():
            where["owner_id"] = owner_id
        rows = self.db.rows(page=1, dump=dump, orderby="updated", order="DESC", **where)
        return rows

    def stats(self, owner_id):
        rows, total = self.search(owner_id, page=None, dump=1000)
        companies = set()
        missing_contact = 0
        for row in rows:
            if row.get("company"):
                companies.add(row.get("company"))
            if not row.get("mobile") and not row.get("email"):
                missing_contact += 1
        return dict(total=total, companies=len(companies), missing_contact=missing_contact)

    def _iso(self, value):
        if not value:
            return ""
        if isinstance(value, str):
            return value.replace(" ", "T") + ("Z" if "T" in value and not value.endswith("Z") else "")
        return value.strftime("%Y-%m-%dT%H:%M:%SZ")

    def _parse_since(self, value):
        value = str(value or "").strip()
        if not value:
            return None
        for candidate in [value, value.replace("Z", ""), value.replace("T", " ").replace("Z", "")]:
            try:
                return datetime.datetime.fromisoformat(candidate)
            except Exception:
                pass
        return None

    def _digest(self, value):
        return hashlib.sha256(str(value or "").encode("utf-8")).hexdigest()

    def _image_hash(self, value):
        value = str(value or "").strip()
        return self._digest(value) if self._has_image_data(value) else ""

    def _has_image_data(self, value):
        return self.DATA_URL_RE.match(str(value or "").strip()) is not None

    def _number_aliases(self, raw):
        text = str(raw or "").strip()
        if not text:
            return []
        text = re.sub(r"^tel:\s*", "", text, flags=re.I)
        text = re.split(r"(?:ext\.?|extension|내선|#)", text, maxsplit=1, flags=re.I)[0]
        digits = "".join([char for char in text if char.isdigit()])
        if not digits:
            return []
        aliases = [digits]
        if digits.startswith("82") and len(digits) > 2:
            aliases.append("0" + digits[2:])
        if digits.startswith("0") and len(digits) > 1:
            aliases.append("82" + digits[1:])
        result = []
        for alias in aliases:
            if alias and alias not in result:
                result.append(alias)
        return result

    def _phone_numbers(self, row):
        numbers = []
        for kind in self.PHONE_KINDS:
            display = str(row.get(kind, "") or "").strip()
            aliases = self._number_aliases(display)
            if aliases:
                numbers.append(dict(kind=kind, display_number=display, aliases=aliases))
        return numbers

    def _generated_hash(self, row):
        keys = ["name", "company", "department", "position", "email", "mobile", "phone", "address", "website", "tags"]
        return self._digest("|".join([str(row.get(key, "") or "") for key in keys]) + "|signature")

    def _overlay_kind(self, row):
        if self._has_image_data(row.get("front_image")):
            return "front"
        if self._has_image_data(row.get("back_image")):
            return "back"
        return "generated"

    def _mobile_item(self, row):
        deleted = row.get("status") != "active"
        item = dict(
            id=row.get("id", ""),
            deleted=deleted,
            updated_at=self._iso(row.get("updated")),
        )
        if deleted:
            return item
        item.update(dict(
            name=row.get("name", ""),
            company=row.get("company", ""),
            department=row.get("department", ""),
            position=row.get("position", ""),
            email=row.get("email", ""),
            mobile=row.get("mobile", ""),
            phone=row.get("phone", ""),
            address=row.get("address", ""),
            website=row.get("website", ""),
            tags=row.get("tags", ""),
            memo_preview=str(row.get("memo", "") or "")[:120],
            phone_numbers=self._phone_numbers(row),
            image_hashes=dict(
                front=self._image_hash(row.get("front_image")),
                back=self._image_hash(row.get("back_image")),
                generated=self._generated_hash(row),
            ),
            overlay_image_kind=self._overlay_kind(row),
        ))
        return item

    def mobile_sync(self, owner_id, since="", limit=10000):
        since_dt = self._parse_since(since)

        def query(db, qs):
            if since_dt is not None:
                qs = qs.where(db.updated > since_dt)
            return qs

        rows = self.db.rows(
            owner_id=owner_id,
            query=query,
            page=1,
            dump=limit,
            orderby="updated",
            order="ASC",
        )
        return [self._mobile_item(row) for row in rows]

    def _decode_data_url(self, value):
        match = self.DATA_URL_RE.match(str(value or "").strip())
        if match is None:
            return None
        mime = match.group(1)
        try:
            raw = base64.b64decode(match.group(2))
        except Exception:
            return None
        return mime, raw

    def _font(self, size, bold=False):
        try:
            from PIL import ImageFont
            filename = "SUIT-Bold.otf" if bold else "SUIT-Regular.otf"
            path = wiz.project.fs().abspath(f"src/assets/font/SUIT/{filename}")
            return ImageFont.truetype(path, size)
        except Exception:
            try:
                from PIL import ImageFont
                return ImageFont.load_default()
            except Exception:
                return None

    def _hex_color(self, value, fallback):
        value = str(value or "").strip()
        if re.match(r"^#[0-9a-fA-F]{6}$", value):
            return value
        return fallback

    def _generated_image(self, row):
        from PIL import Image, ImageDraw

        width, height = 1200, 680
        main = self._hex_color(row.get("main_color"), "#123c69")
        accent = self._hex_color(row.get("accent_color"), "#14b8a6")
        image = Image.new("RGB", (width, height), "#f7fbfa")
        draw = ImageDraw.Draw(image)
        draw.rounded_rectangle((34, 34, width - 34, height - 34), radius=34, fill="#ffffff", outline="#d8e6e1", width=2)
        draw.rectangle((34, 34, 134, height - 34), fill=main)
        draw.rectangle((134, 34, 148, height - 34), fill=accent)
        draw.ellipse((width - 230, 74, width - 92, 212), fill=accent)
        draw.ellipse((width - 188, 116, width - 52, 252), outline=main, width=8)

        name = str(row.get("name") or "이름 없음")
        company = str(row.get("company") or "")
        title = " / ".join([part for part in [row.get("department"), row.get("position")] if part])
        contacts = [
            ("M", row.get("mobile", "")),
            ("T", row.get("phone", "")),
            ("E", row.get("email", "")),
            ("W", row.get("website", "")),
            ("A", row.get("address", "")),
        ]

        draw.text((210, 138), name, fill="#17201d", font=self._font(72, bold=True))
        if company:
            draw.text((214, 238), company, fill=main, font=self._font(36, bold=True))
        if title:
            draw.text((214, 294), title, fill="#475569", font=self._font(30))

        y = 404
        for label, value in contacts:
            value = str(value or "").strip()
            if not value:
                continue
            draw.rounded_rectangle((214, y - 8, 254, y + 32), radius=10, fill="#e8f5f2")
            draw.text((226, y), label, fill=main, font=self._font(22, bold=True))
            draw.text((278, y - 2), value[:58], fill="#26312d", font=self._font(26))
            y += 48

        output = io.BytesIO()
        image.save(output, format="PNG")
        return output.getvalue()

    def mobile_overlay_image(self, owner_id, card_id, kind="generated"):
        row = self.db.get(id=card_id, owner_id=owner_id, status="active")
        if row is None:
            return None
        kind = kind if kind in ["front", "back", "generated"] else self._overlay_kind(row)
        if kind in ["front", "back"]:
            data = self._decode_data_url(row.get(f"{kind}_image"))
            if data is not None:
                mime, raw = data
                ext = "jpg" if "jpeg" in mime or "jpg" in mime else "png"
                return dict(kind=kind, mime=mime, data=raw, hash=self._image_hash(row.get(f"{kind}_image")), filename=f"{card_id}-{kind}.{ext}")
            return None
        raw = self._generated_image(row)
        return dict(kind="generated", mime="image/png", data=raw, hash=self._generated_hash(row), filename=f"{card_id}-generated.png")


Model = BusinessCard
