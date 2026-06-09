import { OnInit } from '@angular/core';
import { Service } from '@wiz/libs/portal/season/service';

export class Component implements OnInit {
    public loading: boolean = false;
    public users: any[] = [];
    public total: number = 0;
    public activeAdminCount: number = 0;
    public initialAdminId: string = '';
    public search: any = { text: '', status: 'all', role: 'all', page: 1, dump: 50 };
    public statuses: any[] = [
        { value: 'all', label: '전체' },
        { value: 'pending', label: '승인 대기' },
        { value: 'active', label: '활성' },
        { value: 'blocked', label: '차단' }
    ];
    public roles: any[] = [
        { value: 'all', label: '전체' },
        { value: 'admin', label: '관리자' },
        { value: 'user', label: '사용자' }
    ];

    constructor(public service: Service) { }

    public async ngOnInit() {
        await this.service.init();
        await this.service.auth.allow.role('admin', '/cards');
        await this.load();
    }

    public async load(page: number = 1) {
        this.search.page = page;
        this.loading = true;
        await this.service.render();

        const { code, data } = await wiz.call('list', this.search);
        if (code === 200) {
            this.users = data.rows || [];
            this.total = data.total || 0;
            this.activeAdminCount = data.active_admin_count || 0;
            this.initialAdminId = data.initial_admin_id || '';
        }

        this.loading = false;
        await this.service.render();
    }

    public async filterStatus(status: string) {
        this.search.status = status;
        await this.load(1);
    }

    public async approve(user: any) {
        const { code, data } = await wiz.call('approve', { id: user.id });
        if (code === 200) await this.load(this.search.page);
        else await this.service.modal.error(data.message || '승인에 실패했습니다.');
    }

    public async activate(user: any) {
        const { code, data } = await wiz.call('activate', { id: user.id });
        if (code === 200) await this.load(this.search.page);
        else await this.service.modal.error(data.message || '활성화에 실패했습니다.');
    }

    public async block(user: any) {
        const ok = await this.service.modal.error(`${user.name} 계정을 차단하시겠습니까?`, '취소', '차단');
        if (!ok) return;
        const { code, data } = await wiz.call('block', { id: user.id });
        if (code === 200) await this.load(this.search.page);
        else await this.service.modal.error(data.message || '차단에 실패했습니다.');
    }

    public async updateRole(user: any) {
        if (this.roleLocked(user)) {
            await this.service.modal.warning(this.roleLockMessage(user));
            await this.load(this.search.page);
            return;
        }

        const { code, data } = await wiz.call('update_role', { id: user.id, role: user.role });
        if (code !== 200) {
            await this.service.modal.error(data.message || '권한 변경에 실패했습니다.');
            await this.load(this.search.page);
        }
    }

    public isInitialAdmin(user: any) {
        return user && (user.id === this.initialAdminId || user.memo === 'initial administrator');
    }

    public isSelf(user: any) {
        return user && this.service.auth.session && user.id === this.service.auth.session.id;
    }

    public roleLocked(user: any) {
        if (!user || user.role !== 'admin') return false;
        return this.isSelf(user) || this.isInitialAdmin(user) || (user.status === 'active' && this.activeAdminCount <= 1);
    }

    public roleLockMessage(user: any) {
        if (this.isSelf(user)) return '본인 관리자 권한은 해제할 수 없습니다.';
        if (this.isInitialAdmin(user)) return '초기 관리자는 사용자로 변경할 수 없습니다.';
        if (user && user.status === 'active' && this.activeAdminCount <= 1) return '남은 관리자가 없어 사용자로 변경할 수 없습니다.';
        return '권한을 변경할 수 없습니다.';
    }

    public statusLabel(status: string) {
        const item = this.statuses.find((row) => row.value === status);
        return item ? item.label : status;
    }

    public statusClass(status: string) {
        if (status === 'active') return 'is-active';
        if (status === 'pending') return 'is-pending';
        if (status === 'blocked') return 'is-blocked';
        return 'is-idle';
    }
}
