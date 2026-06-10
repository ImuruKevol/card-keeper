import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "mobile_device"

    id = pw.CharField(max_length=32, primary_key=True)
    user_id = pw.CharField(max_length=32, index=True)
    device_name = pw.CharField(max_length=160, default="")
    platform = pw.CharField(max_length=32, default="android")
    access_token_hash = pw.CharField(max_length=64, unique=True, index=True)
    refresh_token_hash = pw.CharField(max_length=64, unique=True, index=True)
    overlay_settings = pw.TextField(default="")
    ip = pw.CharField(max_length=64, default="")
    user_agent = pw.TextField(default="")
    status = pw.CharField(max_length=16, default="active", index=True)
    created = pw.DateTimeField(default=datetime.datetime.now, index=True)
    updated = pw.DateTimeField(default=datetime.datetime.now)
    last_seen = pw.DateTimeField(default=datetime.datetime.now, index=True)
    access_expires = pw.DateTimeField(index=True)
    refresh_expires = pw.DateTimeField(index=True)
