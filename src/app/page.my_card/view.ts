import { OnInit } from '@angular/core';
import { Service } from '@wiz/libs/portal/season/service';

export class Component implements OnInit {
    public loading: boolean = false;
    public saving: boolean = false;
    public publishing: boolean = false;
    public sharing: boolean = false;
    public copied: boolean = false;
    public card: any = this.emptyCard();
    public shareUrl: string = '';
    public landscapePreviewOpen: boolean = false;
    public landscapePreviewImage: string = '';
    private previewRenderTimer: any = null;
    private landscapeFullscreenRequested: boolean = false;

    constructor(public service: Service) { }

    public async ngOnInit() {
        await this.service.init();
        await this.service.auth.allow('/access');
        await this.load();
    }

    public emptyCard() {
        return {
            id: '', owner_id: '', name: '', company: '', department: '', position: '', email: '',
            mobile: '', phone: '', address: '', website: '', tagline: '', main_color: '#123c69',
            accent_color: '#14b8a6', theme: 'signature', card_image: '', share_token: '',
            public_enabled: false, status: 'active'
        };
    }

    public async load() {
        this.loading = true;
        await this.service.render();
        const { code, data } = await wiz.call('load');
        this.loading = false;
        if (code === 200) {
            this.card = { ...this.emptyCard(), ...(data.card || {}) };
            this.shareUrl = this.makeShareUrl(this.card.share_token);
        } else {
            await this.service.modal.error((data && data.message) || '내 명함 정보를 불러오지 못했습니다.');
        }
        await this.service.render();
        this.schedulePreviewRender();
    }

    public themeOptions() {
        return [
            { key: 'signature', label: '시그니처' },
            { key: 'lattice', label: '모던' },
            { key: 'flow', label: '플로우' },
        ];
    }

    public colorPresets() {
        return [
            { name: '딥 블루', main: '#123c69', accent: '#14b8a6' },
            { name: '차콜 민트', main: '#17201d', accent: '#22c55e' },
            { name: '버건디 골드', main: '#5f1727', accent: '#f59e0b' },
            { name: '인디고 코랄', main: '#3730a3', accent: '#fb7185' },
            { name: '포레스트', main: '#14532d', accent: '#84cc16' },
        ];
    }

    public applyPreset(preset: any) {
        this.card.main_color = preset.main;
        this.card.accent_color = preset.accent;
        this.markDirty();
    }

    public isPresetActive(preset: any) {
        return this.sanitizeColor(this.card.main_color, '') === preset.main.toLowerCase()
            && this.sanitizeColor(this.card.accent_color, '') === preset.accent.toLowerCase();
    }

    public selectTheme(theme: string) {
        this.card.theme = theme;
        this.markDirty();
    }

    public markDirty() {
        this.copied = false;
        this.schedulePreviewRender();
    }

    private schedulePreviewRender() {
        if (this.previewRenderTimer) window.clearTimeout(this.previewRenderTimer);
        this.previewRenderTimer = window.setTimeout(async () => {
            const canvas = document.querySelector('.card-canvas-preview') as HTMLCanvasElement;
            if (!canvas) return;
            const doc: any = document as any;
            if (doc.fonts && doc.fonts.ready) await doc.fonts.ready;
            this.renderCardImage(canvas);
        }, 0);
    }

    public previewStyle() {
        const main = this.sanitizeColor(this.card.main_color, '#123c69');
        const accent = this.sanitizeColor(this.card.accent_color, '#14b8a6');
        return {
            '--card-main': main,
            '--card-accent': accent,
            '--card-main-soft': this.mix(main, '#ffffff', 0.78),
            '--card-accent-soft': this.mix(accent, '#ffffff', 0.72),
            '--card-ink': this.contrastColor(main),
            '--card-muted': this.withAlpha(this.contrastColor(main), 0.74),
        };
    }

    public cardInitials() {
        const name = String(this.card.name || '').trim();
        if (!name) return 'ME';
        const words = name.split(/\s+/).filter((word: string) => !!word);
        if (words.length >= 2) {
            return `${Array.from(words[0])[0] || ''}${Array.from(words[1])[0] || ''}`.toUpperCase();
        }
        return Array.from(name).slice(0, 2).join('').toUpperCase();
    }

    public roleLine() {
        return [this.card.department, this.card.position].filter((value: string) => !!String(value || '').trim()).join(' · ');
    }

    public previewContacts() {
        return [this.card.mobile, this.card.phone, this.card.email, this.card.website]
            .filter((value: string) => !!String(value || '').trim());
    }

    private contactRows() {
        return [
            { label: 'M', value: this.card.mobile },
            { label: 'T', value: this.card.phone },
            { label: 'E', value: this.card.email },
            { label: 'W', value: this.card.website },
            { label: 'A', value: this.card.address },
        ].filter((row: any) => !!String(row.value || '').trim());
    }

    public publicStateLabel() {
        return this.card.public_enabled ? '공개 중' : '비공개';
    }

    public async save(canvas?: HTMLCanvasElement) {
        if (!await this.ensureValid()) return;
        this.saving = true;
        await this.service.render();
        const payload = this.payload(await this.prepareCardImage(canvas));
        const { code, data } = await wiz.call('save', payload);
        this.saving = false;
        if (code === 200) {
            this.card = { ...this.emptyCard(), ...(data.card || {}) };
            this.shareUrl = this.makeShareUrl(this.card.share_token);
            await this.service.modal.success('내 명함이 저장되었습니다.');
        } else {
            await this.service.modal.error((data && data.message) || '내 명함을 저장하지 못했습니다.');
        }
        await this.service.render();
    }

    public async saveDesign(canvas?: HTMLCanvasElement) {
        await this.save(canvas);
    }

    public async downloadCard(canvas?: HTMLCanvasElement) {
        if (!await this.ensureValid()) return;
        const imageData = await this.prepareCardImage(canvas);
        this.card.card_image = imageData;
        this.downloadDataUrl(imageData, `${this.filenameBase()}-business-card.jpg`);
    }

    public async shareImage(canvas?: HTMLCanvasElement) {
        if (!await this.ensureValid()) return;
        this.sharing = true;
        await this.service.render();
        const imageData = await this.prepareCardImage(canvas);
        this.card.card_image = imageData;
        try {
            const blob = await this.dataUrlToBlob(imageData);
            const file = new File([blob], `${this.filenameBase()}-business-card.jpg`, { type: 'image/jpeg' });
            const nav: any = navigator as any;
            if (nav.share && (!nav.canShare || nav.canShare({ files: [file] }))) {
                await nav.share({ files: [file], title: `${this.card.name} 명함` });
            } else {
                await this.service.modal.error('이 브라우저에서는 이미지 공유를 지원하지 않습니다. 이미지 저장 버튼을 사용해주세요.');
            }
        } catch (error) {
            if ((error as any)?.name !== 'AbortError') {
                await this.service.modal.error('이미지 공유를 완료하지 못했습니다.');
            }
        }
        this.sharing = false;
        await this.service.render();
    }

    public async publishCard(canvas?: HTMLCanvasElement) {
        if (!await this.ensureValid()) return;
        this.publishing = true;
        await this.service.render();
        const payload = this.payload(await this.prepareCardImage(canvas));
        const { code, data } = await wiz.call('publish', payload);
        this.publishing = false;
        if (code === 200) {
            this.card = { ...this.emptyCard(), ...(data.card || {}) };
            this.shareUrl = this.makeShareUrl(this.card.share_token);
            await this.copyShareUrl(false);
            await this.service.modal.success('공개 공유 링크가 준비되었습니다.');
        } else {
            await this.service.modal.error((data && data.message) || '공유 링크를 만들지 못했습니다.');
        }
        await this.service.render();
    }

    public async unpublishCard() {
        const ok = await this.service.modal.error('공개 공유 링크를 비활성화하시겠습니까?', '취소', '비활성화');
        if (!ok) return;
        this.publishing = true;
        await this.service.render();
        const { code, data } = await wiz.call('unpublish');
        this.publishing = false;
        if (code === 200) {
            this.card = { ...this.emptyCard(), ...(data.card || {}) };
            this.shareUrl = this.makeShareUrl(this.card.share_token);
        } else {
            await this.service.modal.error((data && data.message) || '공유 링크를 비활성화하지 못했습니다.');
        }
        await this.service.render();
    }

    public async copyShareUrl(showModal: boolean = false) {
        if (!this.shareUrl) return;
        try {
            await navigator.clipboard.writeText(this.shareUrl);
        } catch (error) {
            const input = document.createElement('textarea');
            input.value = this.shareUrl;
            input.style.position = 'fixed';
            input.style.opacity = '0';
            document.body.appendChild(input);
            input.select();
            document.execCommand('copy');
            document.body.removeChild(input);
        }
        this.copied = true;
        if (showModal) await this.service.modal.success('공유 링크를 복사했습니다.');
        await this.service.render();
    }

    public async openLandscapePreview(canvas?: HTMLCanvasElement) {
        if (!this.isMobileViewport()) return;
        this.landscapePreviewImage = await this.prepareCardImage(canvas);
        this.landscapePreviewOpen = true;
        document.body.style.overflow = 'hidden';
        await this.requestLandscapeFullscreen();
        await this.service.render();
    }

    public async closeLandscapePreview() {
        if (!this.landscapePreviewOpen) return;
        this.landscapePreviewOpen = false;
        this.landscapePreviewImage = '';
        document.body.style.overflow = '';
        await this.releaseLandscapeFullscreen();
        await this.service.render();
    }

    private payload(imageData: string = '') {
        return {
            name: this.card.name || '',
            company: this.card.company || '',
            department: this.card.department || '',
            position: this.card.position || '',
            email: this.card.email || '',
            mobile: this.card.mobile || '',
            phone: this.card.phone || '',
            address: this.card.address || '',
            website: this.card.website || '',
            tagline: this.card.tagline || '',
            main_color: this.sanitizeColor(this.card.main_color, '#123c69'),
            accent_color: this.sanitizeColor(this.card.accent_color, '#14b8a6'),
            theme: ['signature', 'lattice', 'flow'].includes(this.card.theme) ? this.card.theme : 'signature',
            card_image: imageData || this.card.card_image || '',
        };
    }

    private async ensureValid() {
        this.card.main_color = this.sanitizeColor(this.card.main_color, '#123c69');
        this.card.accent_color = this.sanitizeColor(this.card.accent_color, '#14b8a6');
        if (!String(this.card.name || '').trim()) {
            await this.service.modal.error('이름을 입력해주세요.');
            return false;
        }
        return true;
    }

    private makeShareUrl(token: string) {
        if (!token) return '';
        return `${window.location.origin}/share/my-card/${encodeURIComponent(token)}`;
    }

    private filenameBase() {
        const name = String(this.card.name || 'my-card').trim().replace(/[\\/:*?"<>|]/g, '-');
        return name || 'my-card';
    }

    private isMobileViewport() {
        return window.matchMedia('(max-width: 1080px), (hover: none) and (pointer: coarse)').matches;
    }

    private async requestLandscapeFullscreen() {
        const root: any = document.documentElement as any;
        try {
            if (root.requestFullscreen && !document.fullscreenElement) {
                await root.requestFullscreen();
                this.landscapeFullscreenRequested = true;
            }
        } catch (error) {
            this.landscapeFullscreenRequested = false;
        }

        try {
            const orientation: any = (screen as any).orientation;
            if (orientation && orientation.lock) await orientation.lock('landscape');
        } catch (error) {
            // Unsupported browsers still use the rotated overlay.
        }
    }

    private async releaseLandscapeFullscreen() {
        try {
            const orientation: any = (screen as any).orientation;
            if (orientation && orientation.unlock) orientation.unlock();
        } catch (error) {
            // Ignore unsupported orientation APIs.
        }

        try {
            if (this.landscapeFullscreenRequested && document.fullscreenElement && document.exitFullscreen) {
                await document.exitFullscreen();
            }
        } catch (error) {
            // Ignore fullscreen cleanup failures.
        }
        this.landscapeFullscreenRequested = false;
    }

    private sanitizeColor(value: string, fallback: string) {
        value = String(value || '').trim().toLowerCase();
        if (/^#[0-9a-f]{6}$/.test(value)) return value;
        return fallback;
    }

    private hexToRgb(hex: string) {
        hex = this.sanitizeColor(hex, '#000000').replace('#', '');
        return {
            r: parseInt(hex.substring(0, 2), 16),
            g: parseInt(hex.substring(2, 4), 16),
            b: parseInt(hex.substring(4, 6), 16),
        };
    }

    private rgbToHex(r: number, g: number, b: number) {
        const toHex = (value: number) => Math.max(0, Math.min(255, Math.round(value))).toString(16).padStart(2, '0');
        return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
    }

    private mix(hexA: string, hexB: string, amount: number) {
        const a = this.hexToRgb(hexA);
        const b = this.hexToRgb(hexB);
        return this.rgbToHex(
            a.r + (b.r - a.r) * amount,
            a.g + (b.g - a.g) * amount,
            a.b + (b.b - a.b) * amount,
        );
    }

    private contrastColor(hex: string) {
        const rgb = this.hexToRgb(hex);
        const luminance = (rgb.r * 299 + rgb.g * 587 + rgb.b * 114) / 1000;
        return luminance > 150 ? '#17201d' : '#ffffff';
    }

    private withAlpha(hex: string, alpha: number) {
        const rgb = this.hexToRgb(hex);
        return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
    }

    private async prepareCardImage(canvas?: HTMLCanvasElement) {
        const doc: any = document as any;
        if (doc.fonts && doc.fonts.ready) await doc.fonts.ready;
        return this.renderCardImage(this.resolveCardCanvas(canvas));
    }

    private renderCardImage(canvas: HTMLCanvasElement) {
        const width = 1200;
        const height = 680;
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) throw new Error('이미지 컨텍스트를 만들 수 없습니다.');

        const main = this.sanitizeColor(this.card.main_color, '#123c69');
        const accent = this.sanitizeColor(this.card.accent_color, '#14b8a6');
        const ink = this.contrastColor(main);
        const muted = this.withAlpha(ink, 0.74);
        const faint = this.withAlpha(ink, 0.18);
        const leftX = 78;
        const rightX = 722;
        const rightWidth = 398;
        const dividerX = 660;
        const panelColor = ink === '#ffffff' ? '#000000' : '#ffffff';

        ctx.clearRect(0, 0, width, height);
        this.drawBackground(ctx, width, height, main, accent, this.card.theme);

        ctx.textAlign = 'left';
        ctx.textBaseline = 'alphabetic';
        this.drawFitText(ctx, this.card.company || 'BUSINESS CARD', leftX, 112, 540, 34, 22, muted, '800');

        ctx.strokeStyle = this.withAlpha(accent, 0.92);
        ctx.lineWidth = 5;
        ctx.beginPath();
        ctx.moveTo(leftX, 146);
        ctx.lineTo(leftX + 132, 146);
        ctx.stroke();

        ctx.fillStyle = ink;
        this.drawFitText(ctx, this.card.name || '이름', leftX, 262, 548, 82, 48, ink, '900');

        const role = this.roleLine();
        if (role) {
            this.drawFitText(ctx, role, leftX + 4, 326, 540, 36, 24, muted, '800');
        }

        if (this.card.tagline) {
            ctx.fillStyle = this.withAlpha(ink, 0.84);
            this.wrapText(ctx, this.card.tagline, leftX + 4, 420, 530, 38, 3, '800 31px SUIT, Arial, sans-serif');
        }

        ctx.fillStyle = this.withAlpha(accent, 0.84);
        this.roundedRect(ctx, leftX, 562, 184, 8, 4);
        ctx.fill();
        ctx.fillStyle = this.withAlpha(ink, 0.20);
        this.roundedRect(ctx, leftX, 584, 388, 6, 3);
        ctx.fill();

        ctx.fillStyle = this.withAlpha(panelColor, ink === '#ffffff' ? 0.10 : 0.46);
        this.roundedRect(ctx, dividerX + 24, 62, 464, 556, 28);
        ctx.fill();

        ctx.strokeStyle = this.withAlpha(ink, 0.20);
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(dividerX, 88);
        ctx.lineTo(dividerX, 592);
        ctx.stroke();

        this.drawFitText(ctx, 'CONTACT', rightX, 116, rightWidth, 30, 22, this.withAlpha(accent, 0.98), '900');
        let y = 178;
        this.contactRows().forEach((row: any) => {
            const used = this.drawContactRow(ctx, row.label, row.value, rightX, y, rightWidth, accent, muted);
            y += Math.max(64, used * 32 + 36);
        });

        return canvas.toDataURL('image/jpeg', 0.9);
    }

    private resolveCardCanvas(canvas?: HTMLCanvasElement) {
        if (canvas) return canvas;
        const preview = document.querySelector('.card-canvas-preview') as HTMLCanvasElement;
        return preview || document.createElement('canvas');
    }

    private drawBackground(ctx: CanvasRenderingContext2D, width: number, height: number, main: string, accent: string, theme: string) {
        const gradient = ctx.createLinearGradient(0, 0, width, height);
        gradient.addColorStop(0, main);
        gradient.addColorStop(0.58, this.mix(main, '#000000', 0.18));
        gradient.addColorStop(1, this.mix(accent, main, 0.44));
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);

        ctx.fillStyle = this.withAlpha('#ffffff', 0.07);
        this.roundedRect(ctx, 34, 34, width - 68, height - 68, 34);
        ctx.fill();

        if (theme === 'lattice') {
            ctx.fillStyle = this.withAlpha('#ffffff', 0.055);
            this.roundedRect(ctx, 72, 76, 500, height - 152, 38);
            ctx.fill();

            ctx.fillStyle = this.withAlpha(accent, 0.15);
            ctx.beginPath();
            ctx.moveTo(0, height - 138);
            ctx.lineTo(width, height - 78);
            ctx.lineTo(width, height);
            ctx.lineTo(0, height);
            ctx.closePath();
            ctx.fill();

            ctx.fillStyle = this.withAlpha('#ffffff', 0.08);
            ctx.beginPath();
            ctx.moveTo(width - 250, 0);
            ctx.lineTo(width, height);
            ctx.lineTo(width - 92, height);
            ctx.lineTo(width - 342, 0);
            ctx.closePath();
            ctx.fill();
        } else if (theme === 'flow') {
            ctx.fillStyle = this.withAlpha(accent, 0.30);
            ctx.beginPath();
            ctx.moveTo(0, height * 0.72);
            ctx.bezierCurveTo(230, height * 0.58, 390, height * 0.90, 640, height * 0.75);
            ctx.bezierCurveTo(850, height * 0.62, 1010, height * 0.78, width, height * 0.60);
            ctx.lineTo(width, height);
            ctx.lineTo(0, height);
            ctx.closePath();
            ctx.fill();

            ctx.fillStyle = this.withAlpha('#ffffff', 0.12);
            ctx.beginPath();
            ctx.moveTo(0, height * 0.82);
            ctx.bezierCurveTo(230, height * 0.74, 420, height * 1.02, 690, height * 0.86);
            ctx.bezierCurveTo(930, height * 0.72, 1060, height * 0.88, width, height * 0.70);
            ctx.lineTo(width, height);
            ctx.lineTo(0, height);
            ctx.closePath();
            ctx.fill();

            ctx.strokeStyle = this.withAlpha('#ffffff', 0.18);
            ctx.lineWidth = 7;
            ctx.beginPath();
            ctx.moveTo(86, height - 120);
            ctx.bezierCurveTo(310, height - 185, 520, height - 70, 760, height - 120);
            ctx.bezierCurveTo(940, height - 158, 1035, height - 120, width - 72, height - 176);
            ctx.stroke();

            ctx.strokeStyle = this.withAlpha(accent, 0.48);
            ctx.lineWidth = 11;
            ctx.beginPath();
            ctx.moveTo(92, height - 88);
            ctx.bezierCurveTo(310, height - 145, 530, height - 44, 748, height - 88);
            ctx.bezierCurveTo(958, height - 132, 1050, height - 72, width - 76, height - 128);
            ctx.stroke();
        } else {
            ctx.fillStyle = this.withAlpha(accent, 0.22);
            ctx.beginPath();
            ctx.moveTo(820, 0);
            ctx.lineTo(width, 0);
            ctx.lineTo(width, 255);
            ctx.lineTo(940, 205);
            ctx.closePath();
            ctx.fill();
            ctx.fillStyle = this.withAlpha('#ffffff', 0.12);
            ctx.beginPath();
            ctx.moveTo(650, height);
            ctx.lineTo(width, height);
            ctx.lineTo(width, 470);
            ctx.lineTo(780, 555);
            ctx.closePath();
            ctx.fill();
        }

        ctx.fillStyle = this.withAlpha(accent, 0.92);
        ctx.fillRect(width - 28, 0, 28, height);
        ctx.fillStyle = this.withAlpha('#ffffff', 0.18);
        ctx.fillRect(width - 58, 0, 12, height);
    }

    private roundedRect(ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) {
        const r = Math.min(radius, width / 2, height / 2);
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.lineTo(x + width - r, y);
        ctx.quadraticCurveTo(x + width, y, x + width, y + r);
        ctx.lineTo(x + width, y + height - r);
        ctx.quadraticCurveTo(x + width, y + height, x + width - r, y + height);
        ctx.lineTo(x + r, y + height);
        ctx.quadraticCurveTo(x, y + height, x, y + height - r);
        ctx.lineTo(x, y + r);
        ctx.quadraticCurveTo(x, y, x + r, y);
        ctx.closePath();
    }

    private drawFitText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, size: number, minSize: number, color: string, weight: string) {
        text = String(text || '').trim();
        let fontSize = size;
        do {
            ctx.font = `${weight} ${fontSize}px SUIT, Arial, sans-serif`;
            if (ctx.measureText(text).width <= maxWidth || fontSize <= minSize) break;
            fontSize -= 2;
        } while (fontSize >= minSize);
        while (text.length > 1 && ctx.measureText(text).width > maxWidth) {
            text = `${Array.from(text).slice(0, -2).join('')}…`;
        }
        ctx.fillStyle = color;
        ctx.fillText(text, x, y);
    }

    private drawContactRow(ctx: CanvasRenderingContext2D, label: string, value: string, x: number, y: number, maxWidth: number, accent: string, color: string) {
        ctx.fillStyle = this.withAlpha(accent, 0.96);
        ctx.font = '900 22px SUIT, Arial, sans-serif';
        ctx.fillText(label, x, y);
        ctx.fillStyle = color;
        return this.wrapText(ctx, value, x + 52, y, maxWidth - 52, 32, label === 'A' ? 3 : 2, '800 28px SUIT, Arial, sans-serif');
    }

    private wrapText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxWidth: number, lineHeight: number, maxLines: number, font: string) {
        ctx.font = font;
        const lines = this.textLines(ctx, text, maxWidth, maxLines);
        lines.forEach((line: string, index: number) => {
            ctx.fillText(line, x, y + index * lineHeight);
        });
        return lines.length;
    }

    private textLines(ctx: CanvasRenderingContext2D, text: string, maxWidth: number, maxLines: number) {
        const tokens = String(text || '').split(/(\s+)/).filter((token: string) => token.length > 0);
        const lines: string[] = [];
        let current = '';

        const pushLine = () => {
            if (!current) return;
            lines.push(current.trim());
            current = '';
        };

        for (const token of tokens) {
            const next = `${current}${token}`;
            if (ctx.measureText(next).width <= maxWidth) {
                current = next;
                continue;
            }

            if (current.trim()) pushLine();

            if (ctx.measureText(token).width <= maxWidth) {
                current = token.trimStart();
                continue;
            }

            let piece = '';
            for (const letter of Array.from(token)) {
                const candidate = `${piece}${letter}`;
                if (ctx.measureText(candidate).width <= maxWidth) {
                    piece = candidate;
                } else {
                    if (piece) lines.push(piece);
                    piece = letter;
                }
                if (lines.length >= maxLines) break;
            }
            current = piece;
            if (lines.length >= maxLines) break;
        }

        if (current.trim() && lines.length < maxLines) pushLine();
        const limited = lines.slice(0, maxLines);
        if (lines.length > maxLines && limited.length > 0) {
            let last = `${limited[limited.length - 1]}…`;
            while (last.length > 1 && ctx.measureText(last).width > maxWidth) {
                last = `${Array.from(last).slice(0, -2).join('')}…`;
            }
            limited[limited.length - 1] = last;
        }
        return limited;
    }

    private downloadDataUrl(dataUrl: string, filename: string) {
        const link = document.createElement('a');
        link.href = dataUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    private async dataUrlToBlob(dataUrl: string) {
        const response = await fetch(dataUrl);
        return await response.blob();
    }
}
