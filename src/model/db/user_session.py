import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "user_session"

    id = pw.CharField(max_length=64, primary_key=True)
    user_id = pw.CharField(max_length=32, index=True)
    ip = pw.CharField(max_length=64, default="")
    user_agent = pw.TextField(default="")
    login_type = pw.CharField(max_length=32, default="login")
    created = pw.DateTimeField(default=datetime.datetime.now, index=True)
    last_active = pw.DateTimeField(default=datetime.datetime.now, index=True)
    is_active = pw.IntegerField(default=1, index=True)
