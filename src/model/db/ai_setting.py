import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "ai_setting"

    id = pw.CharField(max_length=32, primary_key=True)
    provider = pw.CharField(max_length=24, default="openai", index=True)
    model = pw.CharField(max_length=160, default="")
    api_key = pw.TextField(default="")
    base_url = pw.CharField(max_length=255, default="")
    enabled = pw.BooleanField(default=True)
    updated_by = pw.CharField(max_length=32, default="")
    created = pw.DateTimeField(default=datetime.datetime.now)
    updated = pw.DateTimeField(default=datetime.datetime.now, index=True)
