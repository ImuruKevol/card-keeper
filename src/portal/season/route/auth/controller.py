config = wiz.model("portal/season/config")
BASEURI = config.auth_baseuri
LOGOUT_URI = config.auth_logout_uri
LOGIN_URL = config.auth_login_uri


def _no_store():
    wiz.response.headers.set(**{
        "Cache-Control": "no-store, no-cache, must-revalidate, max-age=0",
        "Pragma": "no-cache",
        "Expires": "0",
    })


def _valid_session():
    if wiz.session.get("id") is None:
        return False

    try:
        struct = wiz.model("struct")
        user_id = wiz.session.get("id")
        token = wiz.session.get("session_token", "")
        user = struct.user.get(id=user_id)
        if user is None or user.get("status") != "active":
            wiz.session.clear()
            return False
        if token and struct.user_session.is_revoked(token):
            wiz.session.clear()
            return False
        return True
    except Exception:
        wiz.session.clear()
        return False


if wiz.request.match(f"{BASEURI}/check") is not None:
    _no_store()
    status = _valid_session()
    data = wiz.session.get() if status else {}
    wiz.response.status(200, status=status, session=data)

if wiz.request.match(f"{BASEURI}/logout") is not None:
    _no_store()
    returnTo = wiz.request.query("returnTo", "/access")
    wiz.session.set(returnTo=returnTo)

    if LOGOUT_URI is not None and LOGOUT_URI != f"{BASEURI}/logout":
        wiz.response.redirect(LOGOUT_URI)

    try:
        struct = wiz.model("struct")
        token = wiz.session.get("session_token", "")
        user_id = wiz.session.get("id", "")
        if token:
            struct.user_session.deactivate(token)
        struct.access_log.record(user_id, action="logout", ip=wiz.request.ip(), user_agent=wiz.request.headers("User-Agent", ""))
    except Exception:
        pass

    wiz.session.clear()
    wiz.response.redirect(returnTo)

if wiz.request.match(f"{BASEURI}/login") is not None:
    _no_store()
    if LOGIN_URL is not None and LOGIN_URL != f"{BASEURI}/login":
        wiz.response.redirect(LOGIN_URL)

if config.auth_saml_use:
    wiz.model("portal/season/auth/saml").proceed()

wiz.response.redirect("/cards")
