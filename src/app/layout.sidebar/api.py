struct = wiz.model("struct")


def change_password():
    user_id = wiz.session.get("id", "")
    if not user_id:
        wiz.response.status(401, message="로그인이 필요합니다.")

    current_password = wiz.request.query("current_password", "")
    new_password = wiz.request.query("new_password", "")
    new_password_confirm = wiz.request.query("new_password_confirm", "")

    if not current_password or not new_password or not new_password_confirm:
        wiz.response.status(400, message="현재 비밀번호와 새 비밀번호를 모두 입력해주세요.")
    if len(new_password) < 8:
        wiz.response.status(400, message="새 비밀번호는 8자 이상이어야 합니다.")
    if new_password != new_password_confirm:
        wiz.response.status(400, message="새 비밀번호 확인이 일치하지 않습니다.")
    if current_password == new_password:
        wiz.response.status(400, message="현재 비밀번호와 다른 비밀번호를 입력해주세요.")

    if struct.user.change_password(user_id, current_password, new_password) is False:
        wiz.response.status(400, message="현재 비밀번호가 올바르지 않습니다.")

    try:
        token = wiz.session.get("session_token", "")
        struct.user_session.revoke_all(user_id, except_token=token)
        struct.access_log.record(
            user_id,
            action="password_change",
            ip=wiz.request.ip(),
            user_agent=wiz.request.headers("User-Agent", ""),
        )
    except Exception:
        pass

    wiz.response.status(200, message="비밀번호가 변경되었습니다.")
