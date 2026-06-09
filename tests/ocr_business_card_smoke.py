import importlib.util
from pathlib import Path

from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parents[3]
PROJECT = ROOT / "project" / "main"
DATA = ROOT / "data"
API_PATH = PROJECT / "src" / "app" / "page.cards" / "api.py"


class FakeWiz:
    def model(self, _name):
        return None


def load_api():
    spec = importlib.util.spec_from_file_location("cards_api", API_PATH)
    module = importlib.util.module_from_spec(spec)
    module.wiz = FakeWiz()
    spec.loader.exec_module(module)
    return module


def assert_contains(fields, expected):
    for key, value in expected.items():
        actual = fields.get(key, "")
        if value not in actual:
            raise AssertionError(f"{key}: expected {value!r} in {actual!r}")


def assert_semantic_parser(api):
    korean_text = """
성명: 홍길동
나노하테크 연구소
전략기획본부 / 수석매니저
휴대폰 010 1234 5678
대표번호 02-345-6789
팩스 02-999-9999
이메일 hong@example.co.kr
소재지 서울특별시 강남구 테헤란로 123 나노하타워 8층
홈페이지 nanoha.co.kr
"""
    english_text = """
Name: Jane Doe
Nanoha Labs LLC
Principal Product Manager, Growth Division
Cell +1 415 555 1234
Direct +1 415 555 9876
Fax +1 415 555 0000
Mail jane.doe@nanoha.ai
HQ 55 Market Street Suite 1200 San Francisco CA USA
Web nanoha.ai
"""
    center_text = """
Name: Alex Kim
Cloud AI Center
Senior Engineer
Mobile +82 10 2222 3333
Office +82 2 777 8888
Email alex@orbit.tech
Website orbit.tech
Orbit Technologies Inc.
"""

    korean = api._parse_ocr_text(korean_text)
    assert_contains(korean, {
        "name": "홍길동",
        "company": "나노하테크 연구소",
        "department": "전략기획본부",
        "position": "수석매니저",
        "email": "hong@example.co.kr",
        "mobile": "010-1234-5678",
        "phone": "02-345-6789",
        "address": "테헤란로 123",
        "website": "nanoha.co.kr",
    })

    english = api._parse_ocr_text(english_text)
    assert_contains(english, {
        "name": "Jane Doe",
        "company": "Nanoha Labs",
        "department": "Growth Division",
        "position": "Principal",
        "email": "jane.doe@nanoha.ai",
        "mobile": "+1-415-555-1234",
        "phone": "+1-415-555-9876",
        "address": "Market Street Suite 1200",
        "website": "nanoha.ai",
    })

    center = api._parse_ocr_text(center_text)
    assert_contains(center, {
        "name": "Alex Kim",
        "company": "Orbit Technologies",
        "department": "Cloud AI Center",
        "position": "Senior",
        "email": "alex@orbit.tech",
        "mobile": "+82-10-2222-3333",
        "phone": "+82-2-777-8888",
        "website": "orbit.tech",
    })

    if "9999" in korean.get("phone", "") or "0000" in english.get("phone", ""):
        raise AssertionError("fax number was incorrectly selected as phone")
    print("semantic parser: ok")


def main():
    api = load_api()
    assert_semantic_parser(api)
    cases = [
        (
            "KakaoTalk_20260604_133335556.jpg",
            {
                "name": "권태욱",
                "company": "주식회사 시즌",
                "department": "개발팀",
                "position": "팀장",
                "email": "kwon3286@season.co.kr",
                "mobile": "010-8378-3636",
                "phone": "044-862-9307",
                "address": "한누리대로 219",
                "website": "www.season.co.kr",
            },
        ),
        (
            "KakaoTalk_20260604_133335556_01.jpg",
            {
                "name": "Taewook Kwon",
                "company": "Season Co. Ltd",
                "department": "Software Development Team",
                "position": "Lead",
                "email": "kwon3286@season.co.kr",
                "mobile": "+82-10-8378-3636",
                "phone": "+82-44-862-9307",
                "address": "Republic of Korea",
                "website": "www.season.co.kr",
            },
        ),
    ]

    parsed_results = []
    total_passes = 0
    for filename, expected in cases:
        image = ImageOps.exif_transpose(Image.open(DATA / filename))
        ocr = api._server_ocr(image)
        if not ocr.get("available"):
            raise AssertionError(f"{filename}: server OCR unavailable: {ocr.get('message')}")
        fields = api._parse_ocr_text(ocr.get("text", ""))
        assert_contains(fields, expected)
        parsed_results.append(fields)
        total_passes += ocr.get("passes", 0)
        print(f"{filename}: ok confidence={ocr.get('confidence')} fields={fields}")

    merged = api._merge_ocr_fields(parsed_results)
    assert_contains(merged, {
        "name": "권태욱",
        "company": "주식회사 시즌",
        "department": "개발팀",
        "position": "팀장",
        "email": "kwon3286@season.co.kr",
        "mobile": "010-8378-3636",
        "phone": "044-862-9307",
        "address": "한누리대로 219",
        "website": "www.season.co.kr",
        "tags": "양면",
    })
    if total_passes > 3:
        raise AssertionError(f"expected adaptive OCR to use at most 3 passes, got {total_passes}")
    print(f"merged: ok passes={total_passes} fields={merged}")

    rotated_image = ImageOps.exif_transpose(Image.open(DATA / cases[0][0])).rotate(90, expand=True)
    rotated_ocr = api._server_ocr(rotated_image)
    if not rotated_ocr.get("available"):
        raise AssertionError(f"rotated image: server OCR unavailable: {rotated_ocr.get('message')}")
    if not rotated_ocr.get("rotation"):
        raise AssertionError(f"rotated image: expected non-zero rotation correction, got {rotated_ocr}")
    rotated_fields = api._parse_ocr_text(rotated_ocr.get("text", ""))
    assert_contains(rotated_fields, {
        "name": "권태욱",
        "company": "주식회사 시즌",
        "email": "kwon3286@season.co.kr",
        "mobile": "010-8378-3636",
        "phone": "044-862-9307",
    })
    print(f"rotated image: ok rotation={rotated_ocr.get('rotation')} passes={rotated_ocr.get('passes')} fields={rotated_fields}")


if __name__ == "__main__":
    main()
