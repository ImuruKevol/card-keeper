struct = wiz.model("struct")


def get_setting():
    wiz.response.status(200, providers=struct.ai_setting.providers(), setting=struct.ai_setting.public())


def models():
    provider = wiz.request.query("provider", "")
    api_key = wiz.request.query("api_key", "")
    base_url = wiz.request.query("base_url", "")

    try:
        rows = struct.ai_setting.list_models(provider=provider, api_key=api_key, base_url=base_url)
    except Exception as e:
        wiz.response.status(400, message=str(e))

    wiz.response.status(200, models=rows)


def save():
    data = wiz.request.query()
    try:
        setting = struct.ai_setting.save(data, user_id=wiz.session.get("id", ""))
    except Exception as e:
        wiz.response.status(400, message=str(e))

    wiz.response.status(200, setting=setting)
