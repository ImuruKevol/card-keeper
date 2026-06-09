import { OnInit } from '@angular/core';
import { Service } from '@wiz/libs/portal/season/service';

export class Component implements OnInit {
    public loading: boolean = false;
    public saving: boolean = false;
    public modelLoading: boolean = false;
    public providers: any[] = [];
    public models: any[] = [];
    public setting: any = this.emptySetting();

    constructor(public service: Service) { }

    public async ngOnInit() {
        await this.service.init();
        await this.service.auth.allow.role('admin', '/cards');
        await this.load();
    }

    public emptySetting() {
        return {
            provider: 'openai',
            model: '',
            api_key: '',
            has_api_key: false,
            base_url: '',
            enabled: true,
            clear_api_key: false
        };
    }

    public async load() {
        this.loading = true;
        await this.service.render();

        const { code, data } = await wiz.call('get_setting');
        if (code === 200) {
            this.providers = data.providers || [];
            this.setting = { ...this.emptySetting(), ...(data.setting || {}) };
            if (this.setting.provider === 'ollama' && !this.setting.base_url) {
                this.setting.base_url = 'http://localhost:11434';
            }
        }

        this.loading = false;
        await this.service.render();
    }

    public selectedProvider() {
        return this.providers.find((item: any) => item.value === this.setting.provider) || { label: this.setting.provider };
    }

    public providerRequiresKey() {
        return this.setting.provider !== 'ollama';
    }

    public providerButtonClass(provider: any) {
        return provider.value === this.setting.provider ? 'provider-button is-active' : 'provider-button';
    }

    public modelInList(model: string) {
        return this.models.some((item: any) => item.id === model);
    }

    public apiKeyPlaceholder() {
        return this.setting.has_api_key ? '등록된 Key 유지' : 'API Key';
    }

    public async selectProvider(provider: any) {
        if (!provider || provider.value === this.setting.provider) return;
        this.setting.provider = provider.value;
        this.setting.model = '';
        this.setting.api_key = '';
        this.setting.has_api_key = false;
        this.setting.clear_api_key = false;
        this.setting.base_url = provider.value === 'ollama' ? 'http://localhost:11434' : '';
        this.models = [];
        await this.service.render();
    }

    public async loadModels() {
        if (this.providerRequiresKey() && !this.setting.api_key && !this.setting.has_api_key) {
            await this.service.modal.error('API Key를 입력해주세요.');
            return;
        }

        this.modelLoading = true;
        await this.service.render();

        const { code, data } = await wiz.call('models', {
            provider: this.setting.provider,
            api_key: this.setting.api_key,
            base_url: this.setting.base_url
        });
        this.modelLoading = false;

        if (code === 200) {
            this.models = data.models || [];
            if (this.models.length > 0 && (!this.setting.model || !this.modelInList(this.setting.model))) {
                this.setting.model = this.models[0].id;
            }
        } else {
            await this.service.modal.error(data.message || '모델 목록을 불러오지 못했습니다.');
        }

        await this.service.render();
    }

    public canSave() {
        if (this.saving) return false;
        if (!this.setting.provider) return false;
        if (!this.setting.enabled) return true;
        if (!this.setting.model) return false;
        if (this.providerRequiresKey() && !this.setting.api_key && !this.setting.has_api_key) return false;
        return true;
    }

    public async clearApiKey() {
        this.setting.api_key = '';
        this.setting.has_api_key = false;
        this.setting.clear_api_key = true;
        await this.service.render();
    }

    public async save() {
        if (!this.canSave()) {
            await this.service.modal.error('Provider, API Key, 모델을 확인해주세요.');
            return;
        }

        this.saving = true;
        await this.service.render();

        const { code, data } = await wiz.call('save', this.setting);
        this.saving = false;

        if (code === 200) {
            this.setting = { ...this.emptySetting(), ...(data.setting || {}) };
            await this.service.modal.success('AI 설정이 저장되었습니다.');
        } else {
            await this.service.modal.error(data.message || '설정 저장에 실패했습니다.');
        }
        await this.service.render();
    }
}
