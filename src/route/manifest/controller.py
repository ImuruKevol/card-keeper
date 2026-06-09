import json

config = wiz.model("portal/season/config")

manifest = {
    "id": config.pwa_start_url,
    "name": config.pwa_title,
    "short_name": "명함장",
    "description": "사진 분석 기반 명함 관리 PWA",
    "start_url": config.pwa_start_url,
    "scope": "/",
    "display": config.pwa_display,
    "background_color": config.pwa_background_color,
    "theme_color": config.pwa_theme_color,
    "orientation": config.pwa_orientation,
    "categories": ["business", "productivity"],
    "icons": [
        {
            "src": config.pwa_icon,
            "sizes": "48x48 64x64 128x128 256x256",
            "type": "image/x-icon"
        },
        {
            "src": config.pwa_icon_192,
            "sizes": "192x192",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": config.pwa_icon_512,
            "sizes": "512x512",
            "type": "image/png",
            "purpose": "any maskable"
        }
    ],
    "shortcuts": [
        {
            "name": "명함 등록",
            "short_name": "등록",
            "url": "/cards?action=capture",
            "icons": [{ "src": config.pwa_icon_192, "sizes": "192x192" }]
        }
    ]
}

wiz.response.send(json.dumps(manifest, ensure_ascii=False), content_type="application/manifest+json; charset=utf-8")
