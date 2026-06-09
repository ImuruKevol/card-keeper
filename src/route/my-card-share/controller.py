import html as html_lib

struct = wiz.model("struct")


def _escape(value):
    return html_lib.escape(str(value or ""), quote=True)


def _clean(value):
    return str(value or "").strip()


def _tel_href(value):
    text = "".join([char for char in _clean(value) if char.isdigit() or char == "+"])
    return f"tel:{text}" if text else ""


def _website_href(value):
    text = _clean(value)
    if not text:
        return ""
    if text.lower().startswith(("http://", "https://")):
        return text
    return f"https://{text}"


def _contact_row(label, value, href="", target=""):
    text = _clean(value)
    if not text:
        return ""
    label = _escape(label)
    value = _escape(text)
    if href:
        href = _escape(href)
        target_attr = ' target="_blank" rel="noopener noreferrer"' if target else ""
        value = f'<a href="{href}"{target_attr}>{value}</a>'
    return f"<div><dt>{label}</dt><dd>{value}</dd></div>"


def _not_found():
    wiz.response.status(404)


segment = wiz.request.match("/share/my-card/<path:token>")
if segment is None:
    _not_found()

token = getattr(segment, "token", "")
card = struct.my_card.public_by_token(token)
if card is None:
    _not_found()

name = _escape(card.get("name", ""))
company = _escape(card.get("company", ""))
position = _escape(card.get("position", ""))
image = card.get("card_image", "")
extension = "jpg" if image.startswith("data:image/jpeg") or image.startswith("data:image/jpg") else "png"
alt = _escape(f"{card.get('name', '')} 명함")
subtitle = " · ".join([part for part in [company, position] if part])
subtitle = _escape(subtitle)
contact_html = "".join([
    _contact_row("휴대폰", card.get("mobile", ""), _tel_href(card.get("mobile", ""))),
    _contact_row("전화", card.get("phone", ""), _tel_href(card.get("phone", ""))),
    _contact_row("이메일", card.get("email", ""), f"mailto:{_clean(card.get('email', ''))}" if _clean(card.get("email", "")) else ""),
    _contact_row("웹사이트", card.get("website", ""), _website_href(card.get("website", "")), "blank"),
])
contact_block = f'<dl class="share-contact">{contact_html}</dl>' if contact_html else ""

html = f"""<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{name} 명함</title>
<meta property="og:title" content="{name} 명함">
<meta property="og:type" content="website">
<style>
html, body {{ margin: 0; min-height: 100%; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f4f7f6; color: #17201d; }}
main {{ min-height: 100vh; display: grid; place-items: center; padding: 28px; }}
.share-card {{ width: min(100%, 840px); }}
.card-image {{ display: block; width: 100%; height: auto; border-radius: 8px; box-shadow: 0 24px 70px rgba(17, 24, 39, 0.22); background: #fff; }}
.share-meta {{ display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: 18px; }}
.share-meta h1 {{ margin: 0; font-size: 20px; line-height: 1.25; }}
.share-meta p {{ margin: 5px 0 0; color: #66736d; font-size: 14px; }}
.share-contact {{ display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px 24px; margin: 18px 0 0; padding: 0; }}
.share-contact div {{ min-width: 0; }}
.share-contact dt {{ margin: 0; color: #78857f; font-size: 11px; font-weight: 900; letter-spacing: 0.04em; }}
.share-contact dd {{ margin: 3px 0 0; color: #17201d; font-size: 14px; font-weight: 800; overflow-wrap: anywhere; }}
.share-contact a {{ color: #102a43; text-decoration: underline; text-decoration-thickness: 1px; text-underline-offset: 3px; }}
.download-link {{ display: inline-flex; min-height: 40px; align-items: center; justify-content: center; border-radius: 8px; padding: 0 14px; background: #102a43; color: #fff; font-weight: 800; text-decoration: none; }}
@media (max-width: 640px) {{ main {{ padding: 18px; }} .share-meta {{ align-items: stretch; flex-direction: column; }} .share-contact {{ grid-template-columns: 1fr; }} .download-link {{ width: 100%; }} }}
</style>
</head>
<body>
<main>
  <section class="share-card">
    <img class="card-image" src="{image}" alt="{alt}">
    <div class="share-meta">
      <div>
        <h1>{name}</h1>
        <p>{subtitle}</p>
      </div>
      <a class="download-link" href="{image}" download="{name}-business-card.{extension}">이미지 저장</a>
    </div>
    {contact_block}
  </section>
</main>
</body>
</html>"""

wiz.response.send(html, content_type="text/html; charset=utf-8")
