import { OnInit } from '@angular/core';
import { Service } from '@wiz/libs/portal/season/service';

export class Component implements OnInit {
    public mode: string = 'login';
    public busy: boolean = false;

    public loginData: any = { email: '', password: '' };
    public signupData: any = { name: '', email: '', mobile: '', password: '', password_confirm: '' };

    constructor(public service: Service) { }

    public async ngOnInit() {
        await this.service.init();
        if (await this.service.auth.check()) location.href = '/cards';
        await this.service.render();
    }

    public async setMode(mode: string) {
        this.mode = mode;
        await this.service.render();
    }

    public async alert(message: string, status: string = 'error') {
        return await this.service.modal.show({
            title: '',
            message: message,
            cancel: false,
            actionBtn: status,
            action: '확인',
            status: status
        });
    }

    public async login() {
        if (!this.loginData.email || !this.loginData.password) {
            await this.alert('이메일과 비밀번호를 입력해주세요.');
            return;
        }

        this.busy = true;
        await this.service.render();
        const { code, data } = await wiz.call('login', this.loginData);
        this.busy = false;

        if (code == 200) {
            location.href = data.redirect || '/cards';
            return;
        }
        await this.alert(data.message || '로그인에 실패했습니다.');
        await this.service.render();
    }

    public async signup() {
        if (!this.signupData.name || !this.signupData.email || !this.signupData.password) {
            await this.alert('이름, 이메일, 비밀번호를 입력해주세요.');
            return;
        }
        if (this.signupData.password.length < 8) {
            await this.alert('비밀번호는 8자 이상이어야 합니다.');
            return;
        }
        if (this.signupData.password !== this.signupData.password_confirm) {
            await this.alert('비밀번호 확인이 일치하지 않습니다.');
            return;
        }

        this.busy = true;
        await this.service.render();
        const { code, data } = await wiz.call('signup', this.signupData);
        this.busy = false;

        if (code == 200) {
            await this.alert(data.message, 'success');
            this.loginData.email = this.signupData.email;
            this.loginData.password = '';
            this.signupData = { name: '', email: '', mobile: '', password: '', password_confirm: '' };
            this.mode = 'login';
        } else {
            await this.alert(data.message || '가입 신청에 실패했습니다.');
        }
        await this.service.render();
    }
}
