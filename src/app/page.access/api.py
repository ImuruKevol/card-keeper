session = wiz.model("portal/season/session").use()
struct = wiz.model("struct")


def login():
    email = wiz.request.query("email", "").strip().lower()
    password = wiz.request.query("password", "")

    if not email or not password:
        wiz.response.status(400, message="이메일과 비밀번호를 입력해주세요.")

    user, message = struct.user.authenticate(email, password)
    if user is None:
        wiz.response.status(401, message=message)

    session.set(_login_type="login")
    session.create(user["id"])

    try:
        struct.access_log.record(user["id"], action="login", ip=wiz.request.ip(), user_agent=wiz.request.headers("User-Agent", ""))
    except Exception:
        pass

    wiz.response.status(200, redirect="/cards")


def signup():
    payload = dict(
        name=wiz.request.query("name", ""),
        email=wiz.request.query("email", ""),
        mobile=wiz.request.query("mobile", ""),
        password=wiz.request.query("password", ""),
    )

    try:
        user, bootstrap = struct.user.signup(payload)
    except Exception as e:
        wiz.response.status(400, message=str(e))

    if bootstrap:
        message = "초기 관리자 계정이 생성되었습니다. 바로 로그인할 수 있습니다."
    else:
        message = "가입 신청이 완료되었습니다. 관리자 승인 후 이용할 수 있습니다."
    wiz.response.status(200, user=user, bootstrap=bootstrap, message=message)
