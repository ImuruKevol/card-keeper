import datetime
import json
import re
import time

import requests


PROVIDERS = [
    dict(value="openai", label="OpenAI"),
    dict(value="google", label="Google"),
    dict(value="ollama", label="Ollama"),
]
PROVIDER_LABELS = {item["value"]: item["label"] for item in PROVIDERS}
OCR_FIELDS = ["name", "company", "department", "position", "email", "mobile", "phone", "address", "website", "memo"]
OPENAI_OCR_PATTERNS = [
    r"^gpt-5",
    r"^gpt-4o",
    r"^gpt-4\.1",
    r"^gpt-4\.5",
    r"vision",
]
OPENAI_EXCLUDE_TERMS = [
    "embedding",
    "audio",
    "tts",
    "whisper",
    "dall-e",
    "image",
    "moderation",
    "realtime",
    "search",
    "transcribe",
]
GOOGLE_EXCLUDE_TERMS = ["embedding", "aqa", "tts", "imagen", "veo", "image-generation"]
OLLAMA_VISION_HINTS = ["llava", "bakllava", "moondream", "minicpm-v", "qwen-vl", "qwen2-vl", "qwen2.5vl", "llama3.2-vision", "vision"]
DEFAULT_OLLAMA_URL = "http://localhost:11434"
AI_OCR_MAX_ATTEMPTS = 2
OCR_RESULT_SCHEMA_NAME = "business_card_ocr.v1"
EMAIL_RE = re.compile(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", re.I)
PHONE_RE = re.compile(r"(?:\+\d{1,3}[\s.-]?\d{1,3}[\s.-]?\d{3,4}[\s.-]?\d{4}|0\d{1,2}[\s.-]?\d{3,4}[\s.-]?\d{4}|\d{3}[\s.-]\d{3,4}[\s.-]\d{4})")
URL_RE = re.compile(r"(?<!@)\b(?:https?://)?(?:www\.)?[A-Z0-9-]+(?:\.[A-Z0-9-]+)*\.[A-Z]{2,}(?:/[\w.?#=&%-]*)?", re.I)
FIELD_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "properties": {key: {"type": "string"} for key in OCR_FIELDS},
    "required": OCR_FIELDS,
}
OCR_RESPONSE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "properties": {
        "text": {"type": "string"},
        "confidence": {"type": "integer"},
        "fields": FIELD_SCHEMA,
        "quality_notes": {
            "type": "array",
            "items": {"type": "string"},
        },
    },
    "required": ["text", "confidence", "fields", "quality_notes"],
}


class AiSetting:
    ID = "ocr"

    def __init__(self, core):
        self.core = core
        self.db = core.orm.use("ai_setting")

    def providers(self):
        return PROVIDERS

    def _now(self):
        return datetime.datetime.now()

    def _bool(self, value):
        if isinstance(value, bool):
            return value
        if isinstance(value, int):
            return value != 0
        return str(value).lower() in ["1", "true", "yes", "y", "on"]

    def _default(self):
        return dict(id=self.ID, provider="openai", model="", api_key="", base_url="", enabled=True, updated_by="")

    def _empty_fields(self):
        return {key: "" for key in OCR_FIELDS}

    def _execution_failure(self, message, setting=None, started=None, attempts=0):
        setting = setting or {}
        elapsed_ms = 0
        if started is not None:
            elapsed_ms = round((time.perf_counter() - started) * 1000)
        provider = setting.get("provider", "")
        return dict(
            available=False,
            text="",
            fields=self._empty_fields(),
            confidence=0,
            quality_notes=[message] if message else [],
            quality_passed=False,
            quality_score=0,
            payload_format="json",
            schema_name=OCR_RESULT_SCHEMA_NAME,
            execution_failed=True,
            message=message or "AI OCR 실행 실패",
            engine=f"ai-{provider}" if provider else "ai-unavailable",
            provider=provider,
            provider_label=PROVIDER_LABELS.get(provider, provider),
            model=setting.get("model", ""),
            engine_label=f"{PROVIDER_LABELS.get(provider, provider)} / {setting.get('model', '')}" if provider else "",
            elapsed_ms=elapsed_ms,
            attempts=attempts,
        )

    def _normalize_provider(self, provider):
        provider = (provider or "openai").strip().lower()
        if provider not in PROVIDER_LABELS:
            raise Exception("지원하지 않는 AI Provider입니다.")
        return provider

    def get(self):
        row = self.db.get(id=self.ID)
        data = self._default()
        if row is not None:
            data.update(row)
        data["enabled"] = self._bool(data.get("enabled"))
        return data

    def public(self):
        data = self.get()
        return dict(
            id=data.get("id", self.ID),
            provider=data.get("provider", "openai"),
            model=data.get("model", ""),
            api_key="",
            has_api_key=bool(data.get("api_key")),
            base_url=data.get("base_url", ""),
            enabled=self._bool(data.get("enabled")),
            provider_label=PROVIDER_LABELS.get(data.get("provider"), data.get("provider", "")),
        )

    def save(self, data, user_id=""):
        current = self.get()
        provider = self._normalize_provider(data.get("provider", current.get("provider", "openai")))
        model = str(data.get("model", "") or "").strip()
        base_url = str(data.get("base_url", "") or "").strip()
        api_key = str(data.get("api_key", "") or "").strip()
        clear_api_key = self._bool(data.get("clear_api_key", False))
        enabled = self._bool(data.get("enabled", True))

        if provider == "ollama" and not base_url:
            base_url = DEFAULT_OLLAMA_URL

        if clear_api_key:
            api_key = ""
        elif not api_key and current.get("provider") == provider:
            api_key = current.get("api_key", "")

        if enabled and not model:
            raise Exception("AI 모델을 선택해주세요.")
        if enabled and provider != "ollama" and not api_key:
            raise Exception("API Key를 입력해주세요.")

        now = self._now()
        item = dict(
            id=self.ID,
            provider=provider,
            model=model,
            api_key=api_key,
            base_url=base_url,
            enabled=enabled,
            updated_by=user_id or "",
            updated=now,
        )
        if self.db.get(id=self.ID) is None:
            item["created"] = now
            self.db.insert(item)
        else:
            self.db.update(item, id=self.ID)
        return self.public()

    def active(self):
        data = self.get()
        if self._bool(data.get("enabled")) is False:
            return None
        if not data.get("provider") or not data.get("model"):
            return None
        if data.get("provider") != "ollama" and not data.get("api_key"):
            return None
        if data.get("provider") == "ollama" and not data.get("base_url"):
            data["base_url"] = DEFAULT_OLLAMA_URL
        return data

    def list_models(self, provider=None, api_key="", base_url=""):
        current = self.get()
        provider = self._normalize_provider(provider or current.get("provider"))
        api_key = (api_key or "").strip()
        base_url = (base_url or "").strip()
        if not api_key and current.get("provider") == provider:
            api_key = current.get("api_key", "")
        if not base_url and current.get("provider") == provider:
            base_url = current.get("base_url", "")

        if provider == "openai":
            if not api_key:
                raise Exception("API Key를 입력해주세요.")
            return self._openai_models(api_key)
        if provider == "google":
            if not api_key:
                raise Exception("API Key를 입력해주세요.")
            return self._google_models(api_key)
        if provider == "ollama":
            return self._ollama_models(api_key, base_url or DEFAULT_OLLAMA_URL)
        return []

    def analyze_image(self, image_data, label=""):
        setting = self.active()
        if setting is None:
            return self._execution_failure("AI OCR 설정 없음")

        started = time.perf_counter()
        last_error = ""
        for attempt in range(AI_OCR_MAX_ATTEMPTS):
            try:
                if setting["provider"] == "openai":
                    raw_text = self._openai_ocr(setting, image_data, label, attempt=attempt)
                elif setting["provider"] == "google":
                    raw_text = self._google_ocr(setting, image_data, label, attempt=attempt)
                elif setting["provider"] == "ollama":
                    raw_text = self._ollama_ocr(setting, image_data, label, attempt=attempt)
                else:
                    raise Exception("지원하지 않는 AI Provider입니다.")

                payload = self._json_from_text(raw_text)
                normalized = self._normalize_ocr_payload(payload, raw_text)
                normalized.update(dict(
                    engine=f"ai-{setting['provider']}",
                    provider=setting["provider"],
                    provider_label=PROVIDER_LABELS.get(setting["provider"], setting["provider"]),
                    model=setting.get("model", ""),
                    engine_label=f"{PROVIDER_LABELS.get(setting['provider'], setting['provider'])} / {setting.get('model', '')}",
                    elapsed_ms=round((time.perf_counter() - started) * 1000),
                    attempts=attempt + 1,
                ))
                return normalized
            except Exception as e:
                last_error = str(e)

        return self._execution_failure(last_error or "AI OCR 응답 JSON 생성 실패", setting=setting, started=started, attempts=AI_OCR_MAX_ATTEMPTS)

    def _model_items(self, provider, ids):
        unique = []
        seen = set()
        for model_id in ids:
            model_id = str(model_id or "").strip()
            if not model_id or model_id in seen:
                continue
            seen.add(model_id)
            unique.append(dict(id=model_id, label=model_id, provider=provider))
        unique.sort(key=lambda item: item["id"])
        return unique

    def _response_error(self, response):
        try:
            data = response.json()
            error = data.get("error")
            if isinstance(error, dict) and error.get("message"):
                return error.get("message")
            if isinstance(error, str):
                return error
            if data.get("message"):
                return data.get("message")
        except Exception:
            pass
        return response.text[:300] if response.text else f"HTTP {response.status_code}"

    def _openai_ocr_model(self, model_id):
        value = model_id.lower()
        if any(term in value for term in OPENAI_EXCLUDE_TERMS):
            return False
        return any(re.search(pattern, value) for pattern in OPENAI_OCR_PATTERNS)

    def _openai_models(self, api_key):
        response = requests.get(
            "https://api.openai.com/v1/models",
            headers={"Authorization": f"Bearer {api_key}"},
            timeout=20,
        )
        if response.status_code >= 400:
            raise Exception(self._response_error(response))
        ids = [item.get("id", "") for item in response.json().get("data", [])]
        ids = [model_id for model_id in ids if self._openai_ocr_model(model_id)]
        return self._model_items("openai", ids)

    def _google_ocr_model(self, model):
        name = str(model.get("name", "")).replace("models/", "")
        lower = name.lower()
        methods = model.get("supportedGenerationMethods", [])
        if "generateContent" not in methods:
            return False
        if not lower.startswith("gemini"):
            return False
        if any(term in lower for term in GOOGLE_EXCLUDE_TERMS):
            return False
        return bool(re.search(r"vision|^gemini-1\.5|^gemini-2|^gemini-3", lower))

    def _google_models(self, api_key):
        response = requests.get(
            "https://generativelanguage.googleapis.com/v1beta/models",
            params={"key": api_key},
            timeout=20,
        )
        if response.status_code >= 400:
            raise Exception(self._response_error(response))
        ids = [
            item.get("name", "").replace("models/", "")
            for item in response.json().get("models", [])
            if self._google_ocr_model(item)
        ]
        return self._model_items("google", ids)

    def _ollama_headers(self, api_key):
        headers = {"Content-Type": "application/json"}
        if api_key:
            headers["Authorization"] = f"Bearer {api_key}"
        return headers

    def _normalize_base_url(self, base_url):
        return (base_url or DEFAULT_OLLAMA_URL).strip().rstrip("/")

    def _ollama_name_looks_vision(self, name):
        lower = name.lower()
        return any(hint in lower for hint in OLLAMA_VISION_HINTS)

    def _ollama_show_has_vision(self, base_url, api_key, name):
        try:
            response = requests.post(
                f"{base_url}/api/show",
                headers=self._ollama_headers(api_key),
                json={"model": name},
                timeout=12,
            )
            if response.status_code >= 400:
                return False
            data = response.json()
            capabilities = [str(item).lower() for item in data.get("capabilities", [])]
            if "vision" in capabilities:
                return True
            encoded = json.dumps(data.get("model_info", {}), ensure_ascii=False).lower()
            return "vision" in encoded or "mmproj" in encoded or "clip" in encoded
        except Exception:
            return False

    def _ollama_models(self, api_key, base_url):
        base_url = self._normalize_base_url(base_url)
        response = requests.get(
            f"{base_url}/api/tags",
            headers=self._ollama_headers(api_key),
            timeout=12,
        )
        if response.status_code >= 400:
            raise Exception(self._response_error(response))
        ids = []
        for item in response.json().get("models", []):
            name = item.get("name", "")
            if self._ollama_name_looks_vision(name) or self._ollama_show_has_vision(base_url, api_key, name):
                ids.append(name)
        return self._model_items("ollama", ids)

    def _schema_for_google(self, schema):
        if isinstance(schema, dict) is False:
            return schema
        converted = {}
        for key, value in schema.items():
            if key == "type" and isinstance(value, str):
                converted[key] = {
                    "object": "OBJECT",
                    "array": "ARRAY",
                    "string": "STRING",
                    "integer": "INTEGER",
                    "number": "NUMBER",
                    "boolean": "BOOLEAN",
                }.get(value, value.upper())
            elif key == "properties":
                converted[key] = {prop: self._schema_for_google(prop_schema) for prop, prop_schema in value.items()}
            elif key == "items":
                converted[key] = self._schema_for_google(value)
            elif key == "additionalProperties":
                continue
            else:
                converted[key] = value
        return converted

    def _openai_response_format(self, structured=True):
        if structured:
            return {
                "type": "json_schema",
                "json_schema": {
                    "name": "business_card_ocr",
                    "strict": True,
                    "schema": OCR_RESPONSE_SCHEMA,
                },
            }
        return {"type": "json_object"}

    def _google_generation_config(self, structured=True):
        config = {
            "temperature": 0,
            "responseMimeType": "application/json",
        }
        if structured:
            config["responseSchema"] = self._schema_for_google(OCR_RESPONSE_SCHEMA)
        return config

    def _ollama_format(self, structured=True):
        return OCR_RESPONSE_SCHEMA if structured else "json"

    def _structured_format_error(self, response):
        message = self._response_error(response).lower()
        return any(term in message for term in ["response_format", "responseformat", "json_schema", "responseschema", "schema", "format"])

    def _ocr_system_prompt(self):
        return (
            "You are a precise OCR and information extraction engine for a Korean business card management service. "
            "Return only a JSON object that matches the provided schema. Do not add markdown. Do not invent values. "
            "If a value is not visible or is ambiguous, use an empty string and add a short quality note."
        )

    def _ocr_prompt(self, label="", attempt=0):
        side = f"현재 이미지는 명함의 {label}입니다." if label else "현재 이미지는 명함 이미지입니다."
        retry_rule = ""
        if attempt > 0:
            retry_rule = "이전 응답이 서비스 저장 기준을 충족하지 못했습니다. 누락된 핵심 필드를 다시 확인하세요.\n"
        return (
            f"{side}\n"
            f"{retry_rule}"
            "명함 관리 서비스에 저장할 정보를 추출하세요.\n"
            "반드시 JSON만 반환하고, 최상위 키는 text, confidence, fields, quality_notes 입니다.\n"
            "text: 이미지에서 보이는 모든 텍스트 줄을 가능한 순서대로 줄바꿈을 유지해 기록합니다.\n"
            "confidence: 0부터 100까지 정수입니다. 추측이 많으면 낮게 설정합니다.\n"
            "fields.name: 사람 이름만 입력합니다. 회사명, 부서명, 직책을 이름으로 쓰지 않습니다.\n"
            "fields.company: 회사/기관/브랜드명입니다. 주식회사, (주), Co. Ltd 등 법인 표기는 보이는 대로 포함합니다.\n"
            "fields.department: 팀/부서/본부/센터/랩 등 조직 단위입니다.\n"
            "fields.position: 대표, 팀장, Lead, Manager, Engineer 같은 직책/역할입니다.\n"
            "fields.email: 이메일 주소 1개입니다.\n"
            "fields.mobile: 휴대폰/모바일/Cell/H.P./M으로 표시된 번호입니다. 한국 휴대폰은 010 형식을 우선합니다.\n"
            "fields.phone: 대표전화/Tel/Office/Direct/Main 번호입니다. Fax 번호는 제외합니다.\n"
            "fields.address: 우편번호를 제외한 주소를 한 줄로 합칩니다.\n"
            "fields.website: 홈페이지/웹사이트/도메인 1개입니다. 이메일 도메인을 웹사이트로 복사하지 않습니다.\n"
            "fields.memo: 저장 가치가 있는 추가 정보가 보일 때만 입력합니다.\n"
            "quality_notes: 흐림, 잘림, 손가림, 낮은 확신, 필드 미확인 등 검증 메모 배열입니다.\n"
            "전화번호와 이메일, 웹사이트는 서로 다른 필드에 중복 저장하지 마세요."
        )

    def _image_parts(self, image_data):
        image_data = image_data or ""
        mime = "image/jpeg"
        raw = image_data
        if image_data.startswith("data:") and "," in image_data:
            header, raw = image_data.split(",", 1)
            match = re.match(r"data:([^;]+)", header)
            if match:
                mime = match.group(1)
        return mime, raw

    def _openai_ocr(self, setting, image_data, label, attempt=0):
        payload = {
            "model": setting.get("model"),
            "messages": [
                {"role": "system", "content": self._ocr_system_prompt()},
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": self._ocr_prompt(label, attempt=attempt)},
                        {"type": "image_url", "image_url": {"url": image_data, "detail": "high"}},
                    ],
                },
            ],
            "response_format": self._openai_response_format(structured=True),
        }
        response = requests.post(
            "https://api.openai.com/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {setting.get('api_key', '')}",
                "Content-Type": "application/json",
            },
            json=payload,
            timeout=45,
        )
        if response.status_code >= 400 and self._structured_format_error(response):
            payload["response_format"] = self._openai_response_format(structured=False)
            response = requests.post(
                "https://api.openai.com/v1/chat/completions",
                headers={
                    "Authorization": f"Bearer {setting.get('api_key', '')}",
                    "Content-Type": "application/json",
                },
                json=payload,
                timeout=45,
            )
        if response.status_code >= 400:
            raise Exception(self._response_error(response))
        choices = response.json().get("choices", [])
        if not choices:
            raise Exception("OpenAI 응답이 비어 있습니다.")
        choice = choices[0]
        message = choice.get("message", {})
        if message.get("refusal"):
            raise Exception(f"OpenAI refusal: {message.get('refusal')}")
        if choice.get("finish_reason") == "length":
            raise Exception("OpenAI 응답이 길이 제한으로 잘렸습니다.")
        return message.get("content", "")

    def _google_ocr(self, setting, image_data, label, attempt=0):
        mime, raw = self._image_parts(image_data)
        body = {
            "contents": [{
                "role": "user",
                "parts": [
                    {"text": f"{self._ocr_system_prompt()}\n\n{self._ocr_prompt(label, attempt=attempt)}"},
                    {"inline_data": {"mime_type": mime, "data": raw}},
                ],
            }],
            "generationConfig": self._google_generation_config(structured=True),
        }
        response = requests.post(
            f"https://generativelanguage.googleapis.com/v1beta/models/{setting.get('model')}:generateContent",
            params={"key": setting.get("api_key", "")},
            json=body,
            timeout=45,
        )
        if response.status_code >= 400 and self._structured_format_error(response):
            body["generationConfig"] = self._google_generation_config(structured=False)
            response = requests.post(
                f"https://generativelanguage.googleapis.com/v1beta/models/{setting.get('model')}:generateContent",
                params={"key": setting.get("api_key", "")},
                json=body,
                timeout=45,
            )
        if response.status_code >= 400:
            raise Exception(self._response_error(response))
        candidates = response.json().get("candidates", [])
        if not candidates:
            raise Exception("Google 응답이 비어 있습니다.")
        finish_reason = candidates[0].get("finishReason", "")
        if finish_reason in ["MAX_TOKENS", "SAFETY", "RECITATION"]:
            raise Exception(f"Google 응답 중단: {finish_reason}")
        parts = candidates[0].get("content", {}).get("parts", [])
        return "".join([part.get("text", "") for part in parts])

    def _ollama_ocr(self, setting, image_data, label, attempt=0):
        _mime, raw = self._image_parts(image_data)
        base_url = self._normalize_base_url(setting.get("base_url"))
        body = {
            "model": setting.get("model"),
            "prompt": f"{self._ocr_system_prompt()}\n\n{self._ocr_prompt(label, attempt=attempt)}",
            "images": [raw],
            "stream": False,
            "format": self._ollama_format(structured=True),
            "options": {"temperature": 0},
        }
        response = requests.post(
            f"{base_url}/api/generate",
            headers=self._ollama_headers(setting.get("api_key", "")),
            json=body,
            timeout=90,
        )
        if response.status_code >= 400 and self._structured_format_error(response):
            body["format"] = self._ollama_format(structured=False)
            response = requests.post(
                f"{base_url}/api/generate",
                headers=self._ollama_headers(setting.get("api_key", "")),
                json=body,
                timeout=90,
            )
        if response.status_code >= 400:
            raise Exception(self._response_error(response))
        return response.json().get("response", "")

    def _json_from_text(self, text):
        raw = (text or "").strip()
        raw = re.sub(r"^```(?:json)?", "", raw, flags=re.I).strip()
        raw = re.sub(r"```$", "", raw).strip()
        try:
            return json.loads(raw)
        except Exception:
            match = re.search(r"\{.*\}", raw, re.S)
            if match:
                return json.loads(match.group(0))
        raise Exception("AI 응답 JSON 파싱 실패")

    def _clean_value(self, value):
        if isinstance(value, list):
            value = ", ".join([str(item).strip() for item in value if str(item).strip()])
        if value is None:
            return ""
        return re.sub(r"\s+", " ", str(value)).strip(" \t\r\n|•-_:;,.")

    def _normalize_phone(self, value):
        value = self._clean_value(value)
        match = PHONE_RE.search(value)
        if match:
            value = match.group(0)
        value = re.sub(r"(?<=\d)\s+(?=\d)", "-", value)
        return re.sub(r"\s*[-.]\s*", "-", value)

    def _looks_mobile(self, value):
        normalized = re.sub(r"\D", "", value or "")
        return normalized.startswith("01") or normalized.startswith("8210")

    def _normalize_fields(self, fields):
        normalized = {}
        for key in OCR_FIELDS:
            normalized[key] = self._clean_value(fields.get(key, ""))

        email_match = EMAIL_RE.search(normalized.get("email", ""))
        normalized["email"] = email_match.group(0) if email_match else ""

        normalized["mobile"] = self._normalize_phone(normalized.get("mobile", ""))
        normalized["phone"] = self._normalize_phone(normalized.get("phone", ""))
        if not normalized["mobile"] and self._looks_mobile(normalized["phone"]):
            normalized["mobile"] = normalized["phone"]
            normalized["phone"] = ""
        if normalized["phone"] and re.search(r"\bfax\b|팩스", normalized["phone"], re.I):
            normalized["phone"] = ""

        website = normalized.get("website", "")
        website_matches = URL_RE.findall(website)
        if website_matches:
            normalized["website"] = website_matches[0]
        elif EMAIL_RE.search(website):
            normalized["website"] = ""

        return normalized

    def _quality_notes(self, payload):
        notes = payload.get("quality_notes", [])
        if isinstance(notes, list) is False:
            notes = [notes] if notes else []
        return [self._clean_value(note) for note in notes if self._clean_value(note)]

    def _field_quality(self, fields, text):
        notes = []
        score = 0
        has_contact = bool(fields.get("email") or fields.get("mobile") or fields.get("phone"))
        has_org = bool(fields.get("company"))
        has_person = bool(fields.get("name"))
        has_location = bool(fields.get("address"))
        has_web = bool(fields.get("website"))

        if has_person:
            score += 20
        else:
            notes.append("이름 미확인")
        if has_org:
            score += 20
        else:
            notes.append("회사 미확인")
        if has_contact:
            score += 25
        else:
            notes.append("연락처 미확인")
        if has_location:
            score += 10
        if has_web:
            score += 8
        if fields.get("department"):
            score += 5
        if fields.get("position"):
            score += 5
        if len((text or "").strip()) >= 40:
            score += 7

        if fields.get("email") and EMAIL_RE.fullmatch(fields["email"]) is None:
            notes.append("이메일 형식 불일치")
            score -= 15
        for key in ["mobile", "phone"]:
            if fields.get(key) and PHONE_RE.search(fields[key]) is None:
                notes.append(f"{key} 형식 불일치")
                score -= 10
        if fields.get("website") and URL_RE.search(fields["website"]) is None:
            notes.append("웹사이트 형식 불일치")
            score -= 8

        available = score >= 45 and (has_person or has_org) and (has_contact or has_web or has_location)
        return dict(score=max(0, min(100, score)), available=available, notes=notes)

    def _estimate_confidence(self, fields, text):
        filled = sum(1 for key in ["name", "company", "email", "mobile", "phone", "address", "website"] if fields.get(key))
        return min(96, 30 + (filled * 8) + min(len((text or "").strip()) // 240, 6))

    def _normalize_ocr_payload(self, payload, raw_text):
        if isinstance(payload, dict) is False:
            raise Exception("AI 응답 JSON 객체 형식 불일치")

        fields = payload.get("fields", payload if isinstance(payload, dict) else {})
        if isinstance(fields, dict) is False:
            fields = {}

        normalized_fields = self._normalize_fields(fields)

        text = str(payload.get("text", "") or "").strip()
        try:
            confidence = int(float(payload.get("confidence", 0)))
        except Exception:
            confidence = 0
        if confidence <= 0:
            confidence = self._estimate_confidence(normalized_fields, text)

        quality = self._field_quality(normalized_fields, text)
        quality_notes = self._quality_notes(payload)
        quality_notes = quality_notes + [note for note in quality["notes"] if note not in quality_notes]
        if confidence > 0:
            confidence = round((confidence * 0.65) + (quality["score"] * 0.35))
        else:
            confidence = quality["score"]
        if quality["available"] is False:
            confidence = min(confidence, 58)

        return dict(
            available=True,
            text=text,
            fields=normalized_fields,
            confidence=min(100, max(0, confidence)),
            quality_notes=quality_notes,
            quality_passed=quality["available"],
            quality_score=quality["score"],
            payload_format="json",
            schema_name=OCR_RESULT_SCHEMA_NAME,
            execution_failed=False,
            message="AI 분석 완료" if quality["available"] else "AI 분석 완료, 품질 확인 필요",
        )


Model = AiSetting
