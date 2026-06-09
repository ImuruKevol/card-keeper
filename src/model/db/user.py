import datetime
import peewee as pw

orm = wiz.model("portal/season/orm")
base = orm.base("base")


class Model(base):
    class Meta:
        db_table = "user"

    id = pw.CharField(max_length=32, primary_key=True)
    email = pw.CharField(max_length=191, unique=True)
    password = pw.CharField(max_length=200)
    name = pw.CharField(max_length=80)
    mobile = pw.CharField(max_length=32, default="")
    role = pw.CharField(max_length=16, default="user", index=True)
    status = pw.CharField(max_length=16, default="pending", index=True)
    memo = pw.TextField(default="")
    created = pw.DateTimeField(default=datetime.datetime.now, index=True)
    updated = pw.DateTimeField(default=datetime.datetime.now)
    last_access = pw.DateTimeField(null=True)
