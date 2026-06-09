struct = wiz.model("struct")


def list():
    text = wiz.request.query("text", "")
    status = wiz.request.query("status", "all")
    role = wiz.request.query("role", "all")
    page = int(wiz.request.query("page", 1))
    dump = int(wiz.request.query("dump", 50))

    rows, total = struct.user.search(text=text, status=status, role=role, page=page, dump=dump)
    wiz.response.status(
        200,
        rows=rows,
        total=total,
        active_admin_count=struct.user.active_admin_count(),
        initial_admin_id=struct.user.initial_admin_id(),
    )


def approve():
    user_id = wiz.request.query("id", "")
    if not user_id:
        wiz.response.status(400, message="사용자 ID가 필요합니다.")
    user = struct.user.approve(user_id)
    wiz.response.status(200, user=user)


def activate():
    user_id = wiz.request.query("id", "")
    if not user_id:
        wiz.response.status(400, message="사용자 ID가 필요합니다.")
    user = struct.user.activate(user_id)
    wiz.response.status(200, user=user)


def block():
    user_id = wiz.request.query("id", "")
    if not user_id:
        wiz.response.status(400, message="사용자 ID가 필요합니다.")
    if user_id == wiz.session.get("id"):
        wiz.response.status(400, message="본인 계정은 차단할 수 없습니다.")
    user = struct.user.block(user_id)
    wiz.response.status(200, user=user)


def update_role():
    user_id = wiz.request.query("id", "")
    role = wiz.request.query("role", "user")
    current_user_id = wiz.session.get("id")
    if not user_id:
        wiz.response.status(400, message="사용자 ID가 필요합니다.")
    if user_id == current_user_id and role != "admin":
        wiz.response.status(400, message="본인 관리자 권한은 해제할 수 없습니다.")

    try:
        user = struct.user.set_role(user_id, role, current_user_id=current_user_id)
    except Exception as e:
        wiz.response.status(400, message=str(e))

    wiz.response.status(200, user=user)
