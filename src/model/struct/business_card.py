import datetime


class BusinessCard:
    FIELDS = [
        "name", "company", "department", "position", "email", "mobile", "phone",
        "address", "website", "front_image", "back_image", "tags", "memo", "source",
    ]
    IMPORT_FIELDS = [
        "name", "company", "department", "position", "email", "mobile", "phone",
        "address", "website", "tags", "memo", "source",
    ]

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


Model = BusinessCard
