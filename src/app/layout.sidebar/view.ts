import { OnInit } from '@angular/core';
import { Service } from '@wiz/libs/portal/season/service';

export class Component implements OnInit {
    public showPasswordForm: boolean = false;
    public passwordBusy: boolean = false;
    public passwordForm: any = this.emptyPasswordForm();

    constructor(public service: Service) { }

    public async ngOnInit() {
        await this.service.init();
        await this.service.auth.allow('/access');
    }

    public emptyPasswordForm() {
        return { current_password: '', new_password: '', new_password_confirm: '' };
    }

    public async openPasswordForm() {
        this.passwordForm = this.emptyPasswordForm();
        this.showPasswordForm = true;
        await this.service.render();
    }

    public async closePasswordForm() {
        if (this.passwordBusy) return;
        this.showPasswordForm = false;
        this.passwordForm = this.emptyPasswordForm();
        await this.service.render();
    }

    public async changePassword() {
        if (!this.passwordForm.current_password || !this.passwordForm.new_password || !this.passwordForm.new_password_confirm) {
            await this.service.modal.error('현재 비밀번호와 새 비밀번호를 모두 입력해주세요.');
            return;
        }
        if (this.passwordForm.new_password.length < 8) {
            await this.service.modal.error('새 비밀번호는 8자 이상이어야 합니다.');
            return;
        }
        if (this.passwordForm.new_password !== this.passwordForm.new_password_confirm) {
            await this.service.modal.error('새 비밀번호 확인이 일치하지 않습니다.');
            return;
        }

        this.passwordBusy = true;
        await this.service.render();
        const { code, data } = await wiz.call('change_password', this.passwordForm);
        this.passwordBusy = false;

        if (code === 200) {
            this.showPasswordForm = false;
            this.passwordForm = this.emptyPasswordForm();
            await this.service.modal.success(data.message || '비밀번호가 변경되었습니다.');
        } else {
            await this.service.modal.error(data.message || '비밀번호 변경에 실패했습니다.');
        }
        await this.service.render();
    }
}
