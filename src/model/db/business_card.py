import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "business_card"

    id = pw.CharField(max_length=32, primary_key=True)
    owner_id = pw.CharField(max_length=32, index=True)
    name = pw.CharField(max_length=120, index=True)
    company = pw.CharField(max_length=160, default="", index=True)
    department = pw.CharField(max_length=120, default="")
    position = pw.CharField(max_length=120, default="")
    email = pw.CharField(max_length=191, default="", index=True)
    mobile = pw.CharField(max_length=64, default="")
    phone = pw.CharField(max_length=64, default="")
    address = pw.CharField(max_length=255, default="")
    website = pw.CharField(max_length=255, default="")
    front_image = base.TextField(default="")
    back_image = base.TextField(default="")
    tags = pw.CharField(max_length=255, default="")
    memo = pw.TextField(default="")
    source = pw.CharField(max_length=32, default="manual")
    status = pw.CharField(max_length=16, default="active", index=True)
    created = pw.DateTimeField(default=datetime.datetime.now, index=True)
    updated = pw.DateTimeField(default=datetime.datetime.now)
