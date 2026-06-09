class Struct:
    def __init__(self):
        self.orm = wiz.model("portal/season/orm")
        self.session = wiz.model("portal/season/session").use()
        self._User = wiz.model("struct/user")
        self._BusinessCard = wiz.model("struct/business_card")
        self._UserSession = wiz.model("struct/user_session")
        self._AccessLog = wiz.model("struct/access_log")
        self._AiSetting = wiz.model("struct/ai_setting")
        self._packages = {}
        self._init_tables()

    def _init_tables(self):
        for name in ["user", "user_session", "access_log", "business_card", "ai_setting"]:
            try:
                db = self.orm.use(name)
                db.orm.create_table(safe=True)
            except Exception:
                pass

    def db(self, name):
        return self.orm.use(name)

    def getUserId(self):
        return self.session.get("id", "")

    def isAdmin(self):
        return self.session.get("role") == "admin"

    @property
    def user(self):
        return self._User(self)

    @property
    def card(self):
        return self._BusinessCard(self)

    @property
    def user_session(self):
        return self._UserSession(self)

    @property
    def access_log(self):
        return self._AccessLog(self)

    @property
    def ai_setting(self):
        return self._AiSetting(self)

    def __getattr__(self, name):
        if name.startswith("_"):
            raise AttributeError(name)
        if name not in self._packages:
            try:
                self._packages[name] = wiz.model(f"portal/{name}/struct")
            except Exception:
                raise AttributeError(f"Package '{name}' not found")
        return self._packages[name]


Model = Struct()
