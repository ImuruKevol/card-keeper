import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "access_log"

    id = pw.CharField(max_length=32, primary_key=True)
    user_id = pw.CharField(max_length=32, index=True)
    action = pw.CharField(max_length=32, index=True)
    ip = pw.CharField(max_length=64, default="")
    user_agent = pw.TextField(default="")
    created = pw.DateTimeField(default=datetime.datetime.now, index=True)
