import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "my_card"

    id = pw.CharField(max_length=32, primary_key=True)
    owner_id = pw.CharField(max_length=32, unique=True, index=True)
    name = pw.CharField(max_length=120, index=True)
    company = pw.CharField(max_length=160, default="")
    department = pw.CharField(max_length=120, default="")
    position = pw.CharField(max_length=120, default="")
    email = pw.CharField(max_length=191, default="")
    mobile = pw.CharField(max_length=64, default="")
    phone = pw.CharField(max_length=64, default="")
    address = pw.CharField(max_length=255, default="")
    website = pw.CharField(max_length=255, default="")
    tagline = pw.CharField(max_length=180, default="")
    main_color = pw.CharField(max_length=16, default="#123c69")
    accent_color = pw.CharField(max_length=16, default="#14b8a6")
    theme = pw.CharField(max_length=32, default="signature")
    card_image = base.TextField(default="")
    share_token = pw.CharField(max_length=64, null=True, unique=True, index=True)
    public_enabled = pw.BooleanField(default=False, index=True)
    status = pw.CharField(max_length=16, default="active", index=True)
    created = pw.DateTimeField(default=datetime.datetime.now, index=True)
    updated = pw.DateTimeField(default=datetime.datetime.now)
