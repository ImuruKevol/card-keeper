import { NgZone, OnDestroy, OnInit } from '@angular/core';
import { Service } from '@wiz/libs/portal/season/service';

export class Component implements OnInit, OnDestroy {
    public loading: boolean = false;
    public saving: boolean = false;
    public analyzing: boolean = false;
    public cards: any[] = [];
    public total: number = 0;
    public allTotal: number = 0;
    public search: any = { text: '', sort: 'name', direction: 'asc', page: 1, dump: 10 };
    public importing: boolean = false;
    public exporting: boolean = false;
    public showExport: boolean = false;
    public exportFormat: string = 'xlsx';
    public showImport: boolean = false;
    public importFileName: string = '';
    public importFileData: string = '';
    public importOptions: any = { delimiter: 'auto', has_header: true, duplicate: 'skip', append_unmapped_to_memo: true };
    public importPreview: any = this.emptyImportPreview();
    public importMappings: any[] = [];
    public importResult: any = null;

    public showForm: boolean = false;
    public formMode: string = 'create';
    public form: any = this.emptyForm();
    public detailOriginal: any = null;
    public copiedField: string = '';
    public activeCaptureSide: string = 'front';
    public captureSlots: any[] = this.emptyCaptureSlots();
    public analysis: any = this.defaultAnalysis();
    public cropper: any = this.emptyCropper();
    private cropperCleanup: any[] = [];
    private cropperFrame: number = 0;

    constructor(public service: Service, private zone: NgZone) { }

    public async ngOnInit() {
        await this.service.init();
        await this.service.auth.allow('/access');
        await this.load();
    }

    public ngOnDestroy() {
        this.releaseCropperListeners();
        this.cancelCropperFrame();
        this.releaseCropperUrl();
    }

    public emptyForm() {
        return {
            id: '', name: '', company: '', department: '', position: '', email: '',
            mobile: '', phone: '', address: '', website: '', front_image: '', back_image: '',
            memo: '', source: 'photo'
        };
    }

    public defaultAnalysis() {
        return { status: 'idle', message: '', confidence: 0, progress: 0, text: '', engine: '' };
    }

    public emptyImportPreview() {
        return {
            filename: '',
            extension: '',
            detected_delimiter: '',
            columns: [],
            mappings: [],
            field_options: [],
            preview: [],
            rows: [],
            total_rows: 0,
            max_rows: 5000,
        };
    }

    public emptyCaptureSlots() {
        return [
            { side: 'front', label: '앞면', preview: '', fileName: '', status: 'empty' },
            { side: 'back', label: '뒷면', preview: '', fileName: '', status: 'empty' }
        ];
    }

    public emptyCropper() {
        return {
            visible: false,
            side: 'front',
            label: '',
            fileName: '',
            objectUrl: '',
            imageUrl: '',
            image: null,
            naturalWidth: 0,
            naturalHeight: 0,
            baseWidth: 0,
            baseHeight: 0,
            minScale: 1,
            maxScale: 4,
            scale: 1,
            offsetX: 0,
            offsetY: 0,
            dragging: false,
            dragPointerId: null,
            dragStartX: 0,
            dragStartY: 0,
            dragOffsetX: 0,
            dragOffsetY: 0,
            stageWidth: 0,
            stageHeight: 0,
            frameLeft: 0,
            frameTop: 0,
            frameWidth: 0,
            frameHeight: 0,
        };
    }

    public captureSlot(side: string) {
        return this.captureSlots.find((slot: any) => slot.side === side) || this.captureSlots[0];
    }

    public activeCaptureSlot() {
        return this.captureSlot(this.activeCaptureSide);
    }

    public capturedSlots() {
        return this.captureSlots.filter((slot: any) => !!slot.preview);
    }

    public capturedImageCount() {
        return this.capturedSlots().length;
    }

    public hasCapturedImages() {
        return this.capturedImageCount() > 0;
    }

    public captureTitle() {
        const names = this.capturedSlots().map((slot: any) => slot.fileName || slot.label);
        return names.length > 0 ? names.join(' / ') : '앞면 / 뒷면';
    }

    public slotClass(slot: any) {
        const classes = ['side-tab'];
        if (slot.side === this.activeCaptureSide) classes.push('is-active');
        if (slot.preview) classes.push('is-ready');
        return classes.join(' ');
    }

    public async selectCaptureSide(side: string) {
        this.activeCaptureSide = side;
        await this.service.render();
    }

    public async startPhotoSelect(side: string, input: HTMLInputElement) {
        if (!this.showForm) {
            this.formMode = 'create';
            this.form = this.emptyForm();
            this.captureSlots = this.emptyCaptureSlots();
            this.analysis = this.defaultAnalysis();
            this.showForm = true;
        }
        this.activeCaptureSide = side;
        input.click();
        await this.service.render();
    }

    public async startImport(input: HTMLInputElement) {
        input.value = '';
        input.click();
        await this.service.render();
    }

    public async onImportSelected(event: any) {
        const input = event.target as HTMLInputElement;
        const files = input.files;
        if (!files || files.length === 0) return;

        const file = files[0];
        input.value = '';
        if (!this.isImportFileAllowed(file)) {
            await this.service.modal.error('CSV, TXT, XLSX 파일만 가져올 수 있습니다.');
            return;
        }
        if (file.size > 6 * 1024 * 1024) {
            await this.service.modal.error('가져오기 파일은 6MB 이하만 사용할 수 있습니다.');
            return;
        }

        this.importing = true;
        this.showImport = true;
        this.importFileName = file.name;
        this.importFileData = '';
        this.importPreview = this.emptyImportPreview();
        this.importMappings = [];
        this.importResult = null;
        this.importOptions = { delimiter: 'auto', has_header: true, duplicate: 'skip', append_unmapped_to_memo: true };
        await this.service.render();

        try {
            this.importFileData = await this.readFileData(file);
            await this.refreshImportPreview();
        } catch (error) {
            this.importing = false;
            this.showImport = false;
            await this.service.render();
            await this.service.modal.error(this.errorMessage(error) || '파일을 읽지 못했습니다.');
        }
    }

    private isImportFileAllowed(file: File) {
        const name = (file.name || '').toLowerCase();
        return ['.csv', '.txt', '.xlsx'].some((extension: string) => name.endsWith(extension));
    }

    private readFileData(file: File): Promise<string> {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(String(reader.result || ''));
            reader.onerror = () => reject(new Error('파일을 읽지 못했습니다.'));
            reader.readAsDataURL(file);
        });
    }

    public importDelimiterOptions() {
        return [
            { key: 'auto', label: '자동' },
            { key: 'comma', label: '쉼표' },
            { key: 'tab', label: '탭' },
            { key: 'semicolon', label: '세미콜론' },
            { key: 'pipe', label: '파이프' },
        ];
    }

    public duplicateOptions() {
        return [
            { key: 'skip', label: '중복 건너뛰기' },
            { key: 'update', label: '기존 항목 업데이트' },
            { key: 'create', label: '새 항목으로 추가' },
        ];
    }

    public importFieldOptions() {
        return this.importPreview.field_options && this.importPreview.field_options.length > 0
            ? this.importPreview.field_options
            : [
                { key: '', label: '가져오지 않음' },
                { key: 'name', label: '이름' },
                { key: 'company', label: '회사' },
                { key: 'department', label: '부서' },
                { key: 'position', label: '직책' },
                { key: 'email', label: '이메일' },
                { key: 'mobile', label: '휴대폰' },
                { key: 'phone', label: '전화' },
                { key: 'address', label: '주소' },
                { key: 'website', label: '웹사이트' },
                { key: 'memo', label: '메모' },
                { key: 'tags', label: '태그' },
                { key: 'source', label: '출처' },
            ];
    }

    public async refreshImportPreview() {
        if (!this.importFileData) return;
        this.importing = true;
        this.importResult = null;
        await this.service.render();

        const { code, data } = await wiz.call('preview_import', {
            filename: this.importFileName,
            file_data: this.importFileData,
            delimiter: this.importOptions.delimiter,
            has_header: this.importOptions.has_header ? 'true' : 'false',
        });

        this.importing = false;
        if (code === 200) {
            this.importPreview = data.preview || this.emptyImportPreview();
            this.importMappings = (this.importPreview.mappings || []).map((item: any) => ({ ...item }));
        } else {
            await this.service.modal.error((data && data.message) || '가져오기 미리보기를 만들지 못했습니다.');
        }
        await this.service.render();
    }

    public async onImportOptionChanged() {
        await this.refreshImportPreview();
    }

    public async setDuplicateMode(mode: string) {
        this.importOptions.duplicate = mode;
        await this.service.render();
    }

    public mappedFieldCount() {
        return this.importMappings.filter((item: any) => !!item.field).length;
    }

    public hasNameMapping() {
        return this.importMappings.some((item: any) => item.field === 'name');
    }

    public importFieldLabel(field: string) {
        const option = this.importFieldOptions().find((item: any) => item.key === field);
        return option ? option.label : field;
    }

    public mappingDuplicateErrors() {
        const grouped: any = {};
        for (const mapping of this.importMappings) {
            if (!mapping.field) continue;
            if (!grouped[mapping.field]) grouped[mapping.field] = [];
            grouped[mapping.field].push(mapping.header || `열 ${mapping.index + 1}`);
        }
        return Object.keys(grouped)
            .filter((field: string) => grouped[field].length > 1)
            .map((field: string) => ({
                field,
                label: this.importFieldLabel(field),
                columns: grouped[field],
            }));
    }

    public mappingErrorMessage() {
        const errors = this.mappingDuplicateErrors();
        if (errors.length === 0) return '';
        return errors
            .map((error: any) => `${error.label}: ${error.columns.join(', ')}`)
            .join(' / ');
    }

    public isMappingDuplicated(field: string) {
        if (!field) return false;
        return this.importMappings.filter((item: any) => item.field === field).length > 1;
    }

    public mappingRowClass(mapping: any) {
        if (mapping && this.isMappingDuplicated(mapping.field)) return 'is-error';
        if (!mapping || !mapping.field) return 'is-unmapped';
        return '';
    }

    public canImport() {
        return !this.importing
            && this.hasNameMapping()
            && this.mappingDuplicateErrors().length === 0
            && !!this.importPreview.total_rows;
    }

    public importPreviewCell(row: any[], index: number) {
        if (!row || index >= row.length) return '';
        return row[index] || '';
    }

    public importColumnSample(index: number) {
        const columns = this.importPreview && this.importPreview.columns ? this.importPreview.columns : [];
        const column = columns[index];
        return column && column.sample ? column.sample : '샘플 없음';
    }

    public importResultColumns() {
        const allowed = ['name', 'company', 'department', 'position', 'email', 'mobile', 'phone', 'address', 'website', 'memo', 'tags', 'source'];
        return this.importFieldOptions().filter((item: any) => allowed.indexOf(item.key) >= 0);
    }

    private importColumnHeader(index: number) {
        const mapping = this.importMappings.find((item: any) => Number(item.index) === Number(index));
        if (mapping && mapping.header) return mapping.header;
        const columns = this.importPreview && this.importPreview.columns ? this.importPreview.columns : [];
        const column = columns.find((item: any) => Number(item.index) === Number(index));
        return column && column.label ? column.label : `열 ${index + 1}`;
    }

    public importMappedPreviewRows() {
        const previewRows = this.importPreview && this.importPreview.preview ? this.importPreview.preview : [];
        const startRow = this.importPreview && this.importPreview.has_header ? 2 : 1;
        return previewRows.map((row: any[], offset: number) => {
            const item: any = { __row: startRow + offset };
            for (const column of this.importResultColumns()) item[column.key] = '';

            for (const mapping of this.importMappings) {
                const index = Number(mapping.index);
                const field = mapping.field;
                if (!field || index >= row.length) continue;
                const value = String(row[index] || '').trim();
                if (value && !item[field]) item[field] = value;
            }

            if (!item.source) item.source = 'import';
            if (this.importOptions.append_unmapped_to_memo) {
                const notes = [];
                for (let index = 0; index < row.length; index++) {
                    const mapping = this.importMappings.find((item: any) => Number(item.index) === index);
                    if (mapping && mapping.field) continue;
                    const value = String(row[index] || '').trim();
                    if (!value) continue;
                    notes.push(`${this.importColumnHeader(index)}: ${value}`);
                }
                if (notes.length > 0) {
                    item.memo = [item.memo, notes.join(' / ')].filter((value: string) => !!value).join(' / ');
                }
            }
            return item;
        });
    }

    public importMappedPreviewCell(row: any, key: string) {
        return row && row[key] ? row[key] : '';
    }

    public async confirmImport() {
        if (!this.importFileData || this.importing) return;
        if (!this.hasNameMapping()) {
            await this.service.modal.error('이름 컬럼을 하나 이상 매핑해야 합니다.');
            return;
        }
        const mappingError = this.mappingErrorMessage();
        if (mappingError) {
            await this.service.modal.error(`중복된 컬럼 매핑을 정리해주세요. ${mappingError}`);
            return;
        }

        this.importing = true;
        await this.service.render();
        const { code, data } = await wiz.call('import_cards', {
            filename: this.importFileName,
            file_data: this.importFileData,
            delimiter: this.importOptions.delimiter,
            has_header: this.importOptions.has_header ? 'true' : 'false',
            duplicate: this.importOptions.duplicate,
            append_unmapped_to_memo: this.importOptions.append_unmapped_to_memo ? 'true' : 'false',
            mappings: JSON.stringify(this.importMappings),
        });
        this.importing = false;

        if (code === 200) {
            this.importResult = data.result || {};
            const created = this.importResult.created || 0;
            const updated = this.importResult.updated || 0;
            const skipped = this.importResult.skipped || 0;
            await this.service.modal.success(`가져오기 완료: 추가 ${created}건, 업데이트 ${updated}건, 건너뜀 ${skipped}건`);
            await this.closeImport();
            await this.load(1, { clearCards: true });
        } else {
            await this.service.modal.error((data && data.message) || '가져오기에 실패했습니다.');
        }
        await this.service.render();
    }

    public async closeImport() {
        this.showImport = false;
        this.importing = false;
        this.importFileName = '';
        this.importFileData = '';
        this.importPreview = this.emptyImportPreview();
        this.importMappings = [];
        this.importResult = null;
        await this.service.render();
    }

    public exportOptions() {
        return [
            { key: 'xlsx', label: 'XLSX', description: '엑셀에서 바로 열 수 있는 통합 문서 형식' },
            { key: 'csv', label: 'CSV', description: '스프레드시트와 다른 시스템에서 쓰기 쉬운 텍스트 형식' },
        ];
    }

    public async openExport() {
        this.exportFormat = 'xlsx';
        this.showExport = true;
        await this.service.render();
    }

    public async closeExport() {
        this.showExport = false;
        await this.service.render();
    }

    public async selectExportFormat(format: string) {
        this.exportFormat = format;
        await this.service.render();
    }

    public async confirmExport() {
        const exported = await this.exportCards(this.exportFormat);
        if (exported) await this.closeExport();
    }

    public async exportCards(format: string) {
        if (this.exporting) return false;
        this.exporting = true;
        await this.service.render();
        const { code, data } = await wiz.call('export_cards', {
            format,
            text: this.search.text || '',
            scope: this.search.scope || 'name_company',
            sort: this.search.sort || 'name',
            direction: this.search.direction || 'asc',
        });
        this.exporting = false;

        if (code === 200) {
            this.downloadBase64(data.data, data.mime, data.filename);
            await this.service.render();
            return true;
        } else {
            await this.service.modal.error((data && data.message) || '내보내기에 실패했습니다.');
        }
        await this.service.render();
        return false;
    }

    private downloadBase64(data: string, mime: string, filename: string) {
        const binary = window.atob(data || '');
        const bytes = new Uint8Array(binary.length);
        for (let index = 0; index < binary.length; index++) {
            bytes[index] = binary.charCodeAt(index);
        }
        const blob = new Blob([bytes], { type: mime || 'application/octet-stream' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename || 'business-cards.xlsx';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    }

    public async load(page: number = 1, options: any = {}) {
        this.search.page = page;
        this.loading = true;
        if (options.clearCards) this.cards = [];
        await this.service.render();
        if (options.scrollToList) this.scrollToListTop();

        const { code, data } = await wiz.call('list', this.search);
        if (code === 200) {
            this.cards = data.rows || [];
            this.total = data.total || 0;
            this.allTotal = data.all_total ?? this.total;
        }

        this.loading = false;
        await this.service.render();
    }

    public totalPages() {
        const dump = Number(this.search.dump) || 10;
        const total = Number(this.total) || 0;
        return Math.max(1, Math.ceil(total / dump));
    }

    public paginationPages() {
        const total = this.totalPages();
        const current = Math.min(Math.max(Number(this.search.page) || 1, 1), total);
        const blockSize = this.paginationBlockSize();
        const start = Math.floor((current - 1) / blockSize) * blockSize + 1;
        const end = Math.min(start + blockSize - 1, total);
        const pages = [];
        for (let page = start; page <= end; page++) pages.push(page);
        return pages;
    }

    public paginationBlockSize() {
        if (typeof window === 'undefined') return 10;
        return window.matchMedia('(max-width: 900px)').matches ? 5 : 10;
    }

    public async goToPage(page: number) {
        const total = this.totalPages();
        const target = Math.min(Math.max(Number(page) || 1, 1), total);
        if (target === Number(this.search.page) && !this.loading) return;
        await this.load(target, { clearCards: true, scrollToList: true });
    }

    public scrollToListTop() {
        if (typeof window === 'undefined' || typeof document === 'undefined') return;
        window.requestAnimationFrame(() => {
            const target = document.getElementById('cardsListTop');
            if (!target) return;
            target.scrollIntoView({ behavior: 'auto', block: 'start', inline: 'nearest' });
        });
    }

    public searchPlaceholder() {
        return '이름 또는 회사명 검색';
    }

    public async clearSearchText() {
        this.search.text = '';
        await this.load(1);
    }

    public async setSort(sort: string) {
        this.search.sort = sort;
        await this.load(1);
    }

    public async setSortDirection(direction: string) {
        this.search.direction = direction;
        await this.load(1);
    }

    public async openCapture() {
        this.formMode = 'create';
        this.form = this.emptyForm();
        this.activeCaptureSide = 'front';
        this.captureSlots = this.emptyCaptureSlots();
        this.analysis = this.defaultAnalysis();
        this.showForm = true;
        await this.service.render();
    }

    public async openEdit(card: any) {
        const editable = await this.fetchEditableCard(card);
        if (!editable) return;

        this.formMode = 'detail';
        this.form = { ...this.emptyForm(), ...editable, tags: '', source: editable.source || 'photo' };
        this.detailOriginal = { ...this.form };
        this.activeCaptureSide = 'front';
        this.captureSlots = this.captureSlotsFromCard(editable);
        this.analysis = { status: 'done', message: '상세 정보를 확인합니다.', confidence: 0, progress: 0, text: '', engine: this.sourceLabel(editable) };
        this.showForm = true;
        await this.service.render();
    }

    private captureSlotsFromCard(card: any) {
        const slots = this.emptyCaptureSlots();
        for (const slot of slots) {
            const image = card && card[`${slot.side}_image`] ? card[`${slot.side}_image`] : '';
            slot.preview = image;
            slot.status = image ? 'ready' : 'empty';
        }
        return slots;
    }

    private async fetchEditableCard(card: any) {
        if (!card || !card.id) return card;
        try {
            const { code, data } = await wiz.call('get', { id: card.id });
            if (code === 200 && data && data.card) return data.card;
            await this.service.modal.error((data && data.message) || '명함 정보를 불러오지 못했습니다.');
        } catch (error) {
            await this.service.modal.error('명함 정보를 불러오지 못했습니다.');
        }
        return null;
    }

    public async closeForm() {
        this.releaseCropperListeners();
        this.cancelCropperFrame();
        this.releaseCropperUrl();
        this.cropper = this.emptyCropper();
        this.showForm = false;
        this.form = this.emptyForm();
        this.detailOriginal = null;
        this.copiedField = '';
        this.activeCaptureSide = 'front';
        this.captureSlots = this.emptyCaptureSlots();
        this.analysis = this.defaultAnalysis();
        await this.service.render();
    }

    public async onPhotoSelected(event: any) {
        const input = event.target as HTMLInputElement;
        const files = input.files;
        if (!files || files.length === 0) return;

        const file = files[0];
        input.value = '';
        if (!file.type || file.type.indexOf('image/') !== 0) {
            await this.service.modal.error('이미지 파일만 등록할 수 있습니다.');
            return;
        }

        const side = this.activeCaptureSide || 'front';
        try {
            await this.openImageCropper(file, side);
        } catch (error) {
            await this.service.modal.error(this.errorMessage(error) || '촬영한 사진을 읽지 못했습니다.');
        }
    }

    public async analyzePhoto() {
        const captured = this.capturedSlots();
        if (captured.length === 0) {
            await this.service.modal.error('명함 이미지를 먼저 선택해주세요.');
            return;
        }

        this.analyzing = true;
        this.analysis = { status: 'analyzing', message: 'AI/서버 분석 중', confidence: 0, progress: 15, text: '', engine: 'AI 우선' };
        await this.service.render();

        const server = await this.analyzeOnServer();
        if (server && server.available) {
            const source = String(server.engine || '').indexOf('ai-') >= 0 ? 'photo-ai' : 'photo-server';
            this.applyParsedFields(server.fields || {}, source);
            this.analysis = {
                status: 'done',
                message: server.message || (server.text && server.text.trim() ? `${captured.length}면 분석 완료` : '인식된 텍스트 없음'),
                confidence: Math.round(server.confidence || 0),
                progress: 100,
                text: server.text || '',
                engine: this.serverEngineLabel(server)
            };
            this.analyzing = false;
            await this.service.render();
            return;
        }

        this.analysis = {
            status: 'failed',
            message: server && server.message ? `분석 실패: ${server.message}` : 'OCR을 사용할 수 없습니다.',
            confidence: 0,
            progress: 0,
            text: '',
            engine: '분석 미가용'
        };
        this.analyzing = false;
        await this.service.render();
    }

    private serverEngineLabel(server: any) {
        if (server && server.engine_label) return server.engine_label;
        const passes = server && server.passes ? ` ${server.passes}패스` : '';
        const rotations = ((server && server.rotations) || []).filter((angle: number) => !!angle);
        const rotationText = rotations.length > 0 ? ` · 회전보정 ${rotations.join(',')}도` : '';
        if (server && String(server.engine || '').indexOf('ai-') >= 0) {
            return `AI${passes}${rotationText}`;
        }
        return `서버${passes}${rotationText}`;
    }

    private async analyzeOnServer() {
        const front = this.captureSlot('front');
        const back = this.captureSlot('back');
        try {
            const { code, data } = await wiz.call('analyze', {
                front_image: front.preview || '',
                front_filename: front.fileName || '',
                back_image: back.preview || '',
                back_filename: back.fileName || ''
            });
            if (code === 200) return data || {};
            return { available: false, message: data && data.message ? data.message : '서버 분석 실패' };
        } catch (error) {
            return { available: false, message: this.errorMessage(error) || '서버 분석 실패' };
        }
    }

    private loadCropImage(file: File): Promise<any> {
        return new Promise((resolve, reject) => {
            const image = new Image();
            const objectUrl = URL.createObjectURL(file);
            image.onload = () => {
                resolve({ image, objectUrl });
            };
            image.onerror = () => {
                URL.revokeObjectURL(objectUrl);
                reject(new Error('촬영한 사진을 읽지 못했습니다. JPG 또는 PNG 이미지로 다시 촬영해주세요.'));
            };
            image.src = objectUrl;
        });
    }

    private async openImageCropper(file: File, side: string) {
        this.releaseCropperListeners();
        this.cancelCropperFrame();
        this.releaseCropperUrl();
        const loaded = await this.loadCropImage(file);
        const slot = this.captureSlot(side);
        this.cropper = {
            ...this.emptyCropper(),
            visible: true,
            side,
            label: slot.label,
            fileName: file.name || `${slot.label}-business-card-image`,
            objectUrl: loaded.objectUrl,
            imageUrl: loaded.objectUrl,
            image: loaded.image,
            naturalWidth: loaded.image.naturalWidth || loaded.image.width,
            naturalHeight: loaded.image.naturalHeight || loaded.image.height,
        };
        await this.service.render();
        this.cropperFrame = window.requestAnimationFrame(() => {
            this.cropperFrame = 0;
            this.initializeCropperViewport();
        });
    }

    private releaseCropperUrl() {
        if (this.cropper && this.cropper.objectUrl) {
            URL.revokeObjectURL(this.cropper.objectUrl);
        }
    }

    private cancelCropperFrame() {
        if (!this.cropperFrame) return;
        window.cancelAnimationFrame(this.cropperFrame);
        this.cropperFrame = 0;
    }

    private cropStageElement() {
        return document.querySelector('.cropper-stage') as HTMLElement;
    }

    private cropFrameElement() {
        return document.querySelector('.crop-frame') as HTMLElement;
    }

    private cropDimElement(name: string) {
        return document.querySelector(`.crop-dim-${name}`) as HTMLElement;
    }

    private cropPhotoElement() {
        return document.querySelector('.cropper-photo') as HTMLElement;
    }

    private cropScaleInputElement() {
        return document.querySelector('.cropper-scale-input') as HTMLInputElement;
    }

    private cropScaleValueElement() {
        return document.querySelector('.cropper-scale-value') as HTMLElement;
    }

    private releaseCropperListeners() {
        for (const cleanup of this.cropperCleanup) {
            try {
                cleanup();
            } catch (error) {
                continue;
            }
        }
        this.cropperCleanup = [];
    }

    private bindCropperControls(stage: HTMLElement) {
        this.releaseCropperListeners();
        const input = this.cropScaleInputElement();

        this.zone.runOutsideAngular(() => {
            const pointerDown = (event: any) => this.startCropDrag(event);
            const pointerMove = (event: any) => this.moveCropDrag(event);
            const pointerEnd = (event: any) => this.endCropDrag(event);
            const wheel = (event: any) => this.onCropWheel(event);
            const inputChange = (event: any) => this.onCropScaleInput(event);
            const resize = () => this.scheduleCropperResize();

            stage.addEventListener('pointerdown', pointerDown);
            stage.addEventListener('pointermove', pointerMove);
            stage.addEventListener('pointerup', pointerEnd);
            stage.addEventListener('pointercancel', pointerEnd);
            stage.addEventListener('lostpointercapture', pointerEnd);
            stage.addEventListener('wheel', wheel, { passive: false });
            window.addEventListener('resize', resize);
            if (input) input.addEventListener('input', inputChange);

            this.cropperCleanup = [
                () => stage.removeEventListener('pointerdown', pointerDown),
                () => stage.removeEventListener('pointermove', pointerMove),
                () => stage.removeEventListener('pointerup', pointerEnd),
                () => stage.removeEventListener('pointercancel', pointerEnd),
                () => stage.removeEventListener('lostpointercapture', pointerEnd),
                () => stage.removeEventListener('wheel', wheel),
                () => window.removeEventListener('resize', resize),
            ];
            if (input) this.cropperCleanup.push(() => input.removeEventListener('input', inputChange));
        });
    }

    private scheduleCropperResize() {
        if (!this.cropper.visible) return;
        if (this.cropperFrame) return;
        this.cropperFrame = window.requestAnimationFrame(() => {
            this.cropperFrame = 0;
            this.initializeCropperViewport();
        });
    }

    private requestCropperSync() {
        if (this.cropperFrame) return;
        this.cropperFrame = window.requestAnimationFrame(() => {
            this.cropperFrame = 0;
            this.syncCropperPhoto();
        });
    }

    private initializeCropperViewport() {
        const stage = this.cropStageElement();
        const frame = this.cropFrameElement();
        if (!stage || !frame || !this.cropper.image) return;

        const stageRect = stage.getBoundingClientRect();
        const frameRect = frame.getBoundingClientRect();
        const naturalWidth = this.cropper.naturalWidth || 1;
        const naturalHeight = this.cropper.naturalHeight || 1;
        const fitScale = Math.min(stageRect.width / naturalWidth, stageRect.height / naturalHeight);
        const baseWidth = Math.max(1, naturalWidth * fitScale);
        const baseHeight = Math.max(1, naturalHeight * fitScale);
        const minScale = Math.max(frameRect.width / baseWidth, frameRect.height / baseHeight, 1);

        this.cropper.baseWidth = baseWidth;
        this.cropper.baseHeight = baseHeight;
        this.cropper.minScale = Number(minScale.toFixed(2));
        this.cropper.maxScale = Number(Math.max(minScale * 4, minScale + 2, 4).toFixed(2));
        this.cropper.scale = this.cropper.minScale;
        this.cropper.offsetX = 0;
        this.cropper.offsetY = 0;
        this.cropper.stageWidth = stageRect.width;
        this.cropper.stageHeight = stageRect.height;
        this.cropper.frameLeft = frameRect.left - stageRect.left;
        this.cropper.frameTop = frameRect.top - stageRect.top;
        this.cropper.frameWidth = frameRect.width;
        this.cropper.frameHeight = frameRect.height;
        this.constrainCropper();
        this.syncCropperMask();
        this.syncCropperPhoto();
        this.bindCropperControls(stage);
    }

    public cropImageTransform() {
        const offsetX = Number(this.cropper.offsetX || 0).toFixed(2);
        const offsetY = Number(this.cropper.offsetY || 0).toFixed(2);
        return `translate3d(calc(-50% + ${offsetX}px), calc(-50% + ${offsetY}px), 0) scale(${this.cropper.scale})`;
    }

    public startCropDrag(event: any) {
        if (!this.cropper.visible) return;
        event.preventDefault();
        this.cropper.dragging = true;
        this.cropper.dragPointerId = event.pointerId;
        this.cropper.dragStartX = event.clientX;
        this.cropper.dragStartY = event.clientY;
        this.cropper.dragOffsetX = this.cropper.offsetX;
        this.cropper.dragOffsetY = this.cropper.offsetY;
        if (event.currentTarget && event.currentTarget.setPointerCapture) {
            event.currentTarget.setPointerCapture(event.pointerId);
        }
    }

    public moveCropDrag(event: any) {
        if (!this.cropper.dragging) return;
        if (this.cropper.dragPointerId !== null && event.pointerId !== this.cropper.dragPointerId) return;
        event.preventDefault();
        this.cropper.offsetX = this.cropper.dragOffsetX + (event.clientX - this.cropper.dragStartX);
        this.cropper.offsetY = this.cropper.dragOffsetY + (event.clientY - this.cropper.dragStartY);
        this.constrainCropper();
        this.requestCropperSync();
    }

    public endCropDrag(event?: any) {
        this.cropper.dragging = false;
        this.cropper.dragPointerId = null;
        if (event && event.currentTarget && event.currentTarget.releasePointerCapture) {
            try {
                event.currentTarget.releasePointerCapture(event.pointerId);
            } catch (error) {
                return;
            }
        }
    }

    public onCropWheel(event: any) {
        if (!this.cropper.visible) return;
        event.preventDefault();
        const factor = event.deltaY < 0 ? 1.06 : 0.94;
        this.setCropScale(this.cropper.scale * factor);
    }

    public onCropScaleInput(event: any) {
        const value = Number(event && event.target ? event.target.value : this.cropper.scale);
        this.setCropScale(value);
    }

    public resetCropper() {
        this.cancelCropperFrame();
        this.initializeCropperViewport();
    }

    private setCropScale(value: number) {
        const next = Math.max(this.cropper.minScale, Math.min(this.cropper.maxScale, value || this.cropper.minScale));
        this.cropper.scale = Number(next.toFixed(2));
        this.constrainCropper();
        this.requestCropperSync();
    }

    private constrainCropper() {
        if (!this.cropper.stageWidth || !this.cropper.stageHeight || !this.cropper.frameWidth || !this.cropper.frameHeight) return;

        const imageWidth = this.cropper.baseWidth * this.cropper.scale;
        const imageHeight = this.cropper.baseHeight * this.cropper.scale;
        const centerX = this.cropper.stageWidth / 2;
        const centerY = this.cropper.stageHeight / 2;
        const frameLeft = this.cropper.frameLeft;
        const frameTop = this.cropper.frameTop;
        const frameRight = frameLeft + this.cropper.frameWidth;
        const frameBottom = frameTop + this.cropper.frameHeight;

        const minOffsetX = frameRight - centerX - imageWidth / 2;
        const maxOffsetX = frameLeft - centerX + imageWidth / 2;
        const minOffsetY = frameBottom - centerY - imageHeight / 2;
        const maxOffsetY = frameTop - centerY + imageHeight / 2;

        this.cropper.offsetX = this.clamp(this.cropper.offsetX, minOffsetX, maxOffsetX);
        this.cropper.offsetY = this.clamp(this.cropper.offsetY, minOffsetY, maxOffsetY);
    }

    private clamp(value: number, min: number, max: number) {
        if (min > max) return (min + max) / 2;
        return Math.max(min, Math.min(max, value));
    }

    private syncCropperPhoto() {
        const photo = this.cropPhotoElement();
        if (photo) {
            const width = `${this.cropper.baseWidth}px`;
            const height = `${this.cropper.baseHeight}px`;
            if (photo.style.width !== width) photo.style.width = width;
            if (photo.style.height !== height) photo.style.height = height;
            photo.style.transform = this.cropImageTransform();
            if (photo.style.opacity !== '1') photo.style.opacity = '1';
        }

        const input = this.cropScaleInputElement();
        if (input) {
            input.min = String(this.cropper.minScale);
            input.max = String(this.cropper.maxScale);
            input.value = String(this.cropper.scale);
        }

        const value = this.cropScaleValueElement();
        if (value) {
            value.textContent = String(this.cropper.scale);
        }
    }

    private syncCropperMask() {
        const stageWidth = Math.max(0, Math.round(this.cropper.stageWidth || 0));
        const stageHeight = Math.max(0, Math.round(this.cropper.stageHeight || 0));
        const frameLeft = Math.max(0, Math.round(this.cropper.frameLeft || 0));
        const frameTop = Math.max(0, Math.round(this.cropper.frameTop || 0));
        const frameWidth = Math.max(0, Math.round(this.cropper.frameWidth || 0));
        const frameHeight = Math.max(0, Math.round(this.cropper.frameHeight || 0));
        const frameRight = Math.min(stageWidth, frameLeft + frameWidth);
        const frameBottom = Math.min(stageHeight, frameTop + frameHeight);

        this.syncCropperDim('top', 0, 0, stageWidth, frameTop);
        this.syncCropperDim('right', frameRight, frameTop, stageWidth - frameRight, frameBottom - frameTop);
        this.syncCropperDim('bottom', 0, frameBottom, stageWidth, stageHeight - frameBottom);
        this.syncCropperDim('left', 0, frameTop, frameLeft, frameBottom - frameTop);
    }

    private syncCropperDim(name: string, x: number, y: number, width: number, height: number) {
        const dim = this.cropDimElement(name);
        if (!dim) return;

        const nextWidth = `${Math.max(0, Math.round(width))}px`;
        const nextHeight = `${Math.max(0, Math.round(height))}px`;
        const nextTransform = `translate3d(${Math.round(x)}px, ${Math.round(y)}px, 0)`;
        if (dim.style.width !== nextWidth) dim.style.width = nextWidth;
        if (dim.style.height !== nextHeight) dim.style.height = nextHeight;
        if (dim.style.transform !== nextTransform) dim.style.transform = nextTransform;
    }

    public async cancelCropper() {
        this.releaseCropperListeners();
        this.cancelCropperFrame();
        this.releaseCropperUrl();
        this.cropper = this.emptyCropper();
        await this.service.render();
    }

    public async applyCropper() {
        try {
            const dataUrl = this.cropSelectedImage();
            const side = this.cropper.side;
            const slot = this.captureSlot(side);
            slot.fileName = this.cropper.fileName;
            slot.preview = dataUrl;
            slot.status = 'ready';
            this.form.source = 'photo';
            this.releaseCropperListeners();
            this.cancelCropperFrame();
            this.releaseCropperUrl();
            this.cropper = this.emptyCropper();

            if (side === 'front' && !this.captureSlot('back').preview) {
                this.activeCaptureSide = 'back';
            }
            const count = this.capturedImageCount();
            this.analysis = { status: 'ready', message: `${count}면 분석 대기`, confidence: 0, progress: 0, text: '', engine: '' };
            await this.service.render();
        } catch (error) {
            await this.service.modal.error(this.errorMessage(error) || '명함 영역을 잘라내지 못했습니다.');
        }
    }

    private cropSelectedImage() {
        const stage = this.cropStageElement();
        const frame = this.cropFrameElement();
        const image = this.cropper.image;
        if (!stage || !frame || !image) throw new Error('명함 영역을 확인할 수 없습니다.');

        const stageRect = stage.getBoundingClientRect();
        const frameRect = frame.getBoundingClientRect();
        const imageWidth = this.cropper.baseWidth * this.cropper.scale;
        const imageHeight = this.cropper.baseHeight * this.cropper.scale;
        const imageLeft = stageRect.width / 2 + this.cropper.offsetX - imageWidth / 2;
        const imageTop = stageRect.height / 2 + this.cropper.offsetY - imageHeight / 2;
        const frameLeft = frameRect.left - stageRect.left;
        const frameTop = frameRect.top - stageRect.top;

        const sourceX = ((frameLeft - imageLeft) / imageWidth) * this.cropper.naturalWidth;
        const sourceY = ((frameTop - imageTop) / imageHeight) * this.cropper.naturalHeight;
        const sourceWidth = (frameRect.width / imageWidth) * this.cropper.naturalWidth;
        const sourceHeight = (frameRect.height / imageHeight) * this.cropper.naturalHeight;
        const safeX = this.clamp(sourceX, 0, this.cropper.naturalWidth - 1);
        const safeY = this.clamp(sourceY, 0, this.cropper.naturalHeight - 1);
        const safeWidth = Math.min(sourceWidth, this.cropper.naturalWidth - safeX);
        const safeHeight = Math.min(sourceHeight, this.cropper.naturalHeight - safeY);
        const maxOutputSide = 2200;
        const outputScale = Math.min(1, maxOutputSide / Math.max(safeWidth, safeHeight));
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(safeWidth * outputScale));
        canvas.height = Math.max(1, Math.round(safeHeight * outputScale));
        const context = canvas.getContext('2d');
        if (!context) throw new Error('이미지 변환 컨텍스트를 만들 수 없습니다.');
        context.drawImage(image, safeX, safeY, safeWidth, safeHeight, 0, 0, canvas.width, canvas.height);
        return this.highQualityDataUrl(canvas);
    }

    private highQualityDataUrl(canvas: HTMLCanvasElement) {
        const maxBytes = 2800 * 1024;
        const qualities = [0.92, 0.88, 0.84];
        let fallback = canvas.toDataURL('image/jpeg', qualities[qualities.length - 1]);
        for (const quality of qualities) {
            const dataUrl = canvas.toDataURL('image/jpeg', quality);
            fallback = dataUrl;
            if (this.imageBytes(dataUrl) <= maxBytes) return dataUrl;
        }
        return fallback;
    }

    private imageBytes(dataUrl: string) {
        const base64 = dataUrl.indexOf(',') >= 0 ? dataUrl.split(',', 2)[1] : dataUrl;
        return Math.ceil((base64.length * 3) / 4);
    }

    private errorMessage(error: any) {
        if (!error) return '';
        if (typeof error === 'string') return error;
        if (error.message) return String(error.message);
        if (error.data && error.data.message) return String(error.data.message);
        if (error.error && error.error.message) return String(error.error.message);
        return '';
    }

    private parseOcrText(text: string) {
        const raw = text || '';
        const emailRegex = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i;
        const phoneRegex = /(?:\+\d{1,3}[\s.-]?\d{1,3}[\s.-]?\d{3,4}[\s.-]?\d{4}|0\d{1,2}[\s.-]?\d{3,4}[\s.-]?\d{4}|\d{3}[\s.-]\d{3,4}[\s.-]\d{4})/g;
        const urlRegex = /(?<!@)\b(?:https?:\/\/)?(?:www\.)?[A-Z0-9-]+(?:\.[A-Z0-9-]+)*\.[A-Z]{2,}(?:[\/\w.?#=&%-]*)?/ig;
        const companyRegex = /주식회사\s*[가-힣A-Za-z0-9]+|\(주\)\s*[가-힣A-Za-z0-9]+|㈜\s*[가-힣A-Za-z0-9]+|유한회사\s*[가-힣A-Za-z0-9]+|합자회사\s*[가-힣A-Za-z0-9]+|합명회사\s*[가-힣A-Za-z0-9]+|재단법인\s*[가-힣A-Za-z0-9]+|사단법인\s*[가-힣A-Za-z0-9]+|[가-힣A-Za-z0-9]+(?:테크|랩스|연구소|스튜디오|파트너스|솔루션|시스템즈|컴퍼니)(?:\s*(?:연구소|랩스|스튜디오))?|[A-Z][A-Za-z0-9&.,\s-]{1,80}?\b(?:Co\.?\s*Ltd\.?|Ltd\.?|Limited|LLC|LLP|Inc\.?|Corp\.?|Corporation|Company|GmbH|Pte\.?\s*Ltd\.?|Pty\.?\s*Ltd\.?|PLC|Group|Labs?|Studio|Partners|Technologies|Systems)/i;
        const companyHintRegex = /회사|그룹|컴퍼니|코퍼레이션|법인|재단|사단|연구소|스튜디오|랩스?|파트너스|솔루션|테크|시스템즈|\b(?:Group|Holdings|Labs?|Studio|Studios|Partners|Solutions|Technologies|Technology|Tech|Systems|Services|Global)\b/i;
        const positionRegex = /대표이사|공동대표|대표|회장|부회장|사장|부사장|전무|상무|이사|감사|고문|실장|팀장|본부장|센터장|부장|차장|과장|대리|주임|사원|수석매니저|선임매니저|책임매니저|프로덕트매니저|프로젝트매니저|책임|수석|선임|연구원|매니저|컨설턴트|디자이너|개발자|엔지니어|Co-Founder|Founder|Owner|President|Vice President|VP|Managing Director|General Manager|Chief\s+[A-Za-z\s]+Officer|CEO|CTO|CFO|COO|CPO|CMO|CIO|Principal|Partner|Senior|Junior|Staff|Associate|Consultant|Specialist|Designer|Developer|Researcher|Analyst|Architect|Product\s+Manager|Project\s+Manager|Sales\s+Manager|Marketing\s+Manager|Manager|Director|Lead|Head|Engineer|Officer/i;
        const roleRegex = /대표이사|공동대표|대표|회장|부회장|사장|부사장|전무|상무|이사|감사|고문|실장|팀장|본부장|센터장|부장|차장|과장|대리|주임|사원|책임|수석|선임|연구원|매니저|컨설턴트|디자이너|개발자|엔지니어|Co-Founder|Founder|Owner|President|Vice President|VP|Managing Director|General Manager|Chief\s+[A-Za-z\s]+Officer|CEO|CTO|CFO|COO|CPO|CMO|CIO|Principal|Partner|Senior|Junior|Staff|Associate|Consultant|Specialist|Designer|Developer|Researcher|Analyst|Architect|Product\s+Manager|Project\s+Manager|Sales\s+Manager|Marketing\s+Manager|Manager|Director|Lead|Head|Engineer|Officer|본부|센터|부서|팀|사업부|부문|실|국|과|파트|랩|연구소|Team|Dept\.?|Department|Division|Office|Center|Centre|Lab|Laboratory|Group|Unit|Part|Squad|Chapter|Software|Product|Design|Planning|Strategy|Sales|Marketing|Operation|Operations|Engineering|Development/i;
        const addressRegex = /\bAddr\.?|\bAddress\b|주소|소재지|위치|자치시|특별시|광역시|시\s|군\s|구\s|동\s|읍\s|면\s|대로|로\s|길\s|번지|호\s|층\s|빌딩|타워|프라자|센터|Republic of Korea|Korea|Hannuri|Hanlim|Suite|\bSte\.?\b|Floor|\bFl\.?\b|\bBldg\.?\b|Building|Road|\bRd\.?\b|Street|\bSt\.?\b|\bAve\.?\b|Avenue|\bBlvd\.?\b|Drive|\bDr\.?\b|\bCity\b|\bDong\b|\bGu\b|\bSi\b/i;
        const addressStrongRegex = /\bAddr\.?|\bAddress\b|\bHQ\b|Headquarters|Location|Located at|주소|소재지|위치|자치시|특별시|광역시|시\s|군\s|구\s|동\s|읍\s|면\s|대로|로\s|길\s|번지|호\s|층\s|빌딩|타워|프라자|Republic of Korea|Korea|Hannuri|Hanlim|Suite|\bSte\.?\b|Floor|\bFl\.?\b|\bBldg\.?\b|Building|Road|\bRd\.?\b|Street|\bSt\.?\b|\bAve\.?\b|Avenue|\bBlvd\.?\b|Drive|\bDr\.?\b|\bCity\b|\bDong\b|\bGu\b|\bSi\b/i;
        const nameLabelRegex = /^(?:성명|이름|담당자|Name|Contact|Person in Charge)\s*[:：.-]?\s*(.+)$/i;
        const mobileLabelRegex = /\bM(?:obile)?\.?\b|\bMob\.?\b|\bCell(?:phone|ular)?\.?\b|\bC\.?P\.?\b|\bH\.?P\.?\b|휴대폰|핸드폰|모바일|연락처|휴대전화|휴대|무선/i;
        const phoneLabelRegex = /\bT(?:el)?\.?\b|\bTelephone\.?\b|\bPhone\.?\b|\bOffice\.?\b|\bDirect\.?\b|\bMain\.?\b|전화번호|전화|대표전화|대표번호|유선|사무실|내선/i;
        const faxLabelRegex = /\bF(?:ax)?\.?\b|팩스/i;

        const cleanLine = (line: string) => {
            return (line || '').replace(/[|•]/g, ' ').replace(/\s+/g, ' ').replace(/^[\s\-_:;,.]+|[\s\-_:;,.]+$/g, '');
        };
        const stripContact = (value: string) => {
            return cleanLine((value || '')
                .replace(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/ig, '')
                .replace(/(?<!@)\b(?:https?:\/\/)?(?:www\.)?[A-Z0-9-]+(?:\.[A-Z0-9-]+)*\.[A-Z]{2,}(?:[\/\w.?#=&%-]*)?/ig, '')
                .replace(/(?:\+\d{1,3}[\s.-]?\d{1,3}[\s.-]?\d{3,4}[\s.-]?\d{4}|0\d{1,2}[\s.-]?\d{3,4}[\s.-]?\d{4}|\d{3}[\s.-]\d{3,4}[\s.-]\d{4})/g, '')
                .replace(/\b(?:M|T|F|Tel|Telephone|Phone|Mobile|Mob|Cell|Fax|Direct|Office|Main|E-mail|Email|Mail|Web|Website|Homepage|Addr|Address)\.?\b|휴대폰|핸드폰|모바일|휴대전화|전화번호|대표전화|대표번호|전화|팩스|이메일|메일|홈페이지|웹사이트|주소|소재지/ig, ' '));
        };
        const cleanCompany = (line: string) => {
            const match = (line || '').match(companyRegex);
            if (match) return stripContact(match[0]);
            const candidate = stripContact(line || '');
            if (candidate && companyHintRegex.test(candidate) && !roleRegex.test(candidate) && !addressStrongRegex.test(candidate) && !isContactLine(candidate)) {
                return candidate;
            }
            return '';
        };
        const extractWebsites = (value: string) => {
            const emailSpans: any[] = [];
            const emailFinder = new RegExp(emailRegex.source, 'ig');
            let emailMatch: RegExpExecArray | null;
            while ((emailMatch = emailFinder.exec(value || '')) !== null) {
                emailSpans.push({ start: emailMatch.index, end: emailMatch.index + emailMatch[0].length });
            }

            const matches: string[] = [];
            const regex = new RegExp(urlRegex.source, 'ig');
            let match: RegExpExecArray | null;
            while ((match = regex.exec(value || '')) !== null) {
                const start = match.index;
                const end = start + match[0].length;
                if (emailSpans.some((span: any) => start >= span.start && end <= span.end)) continue;
                matches.push(match[0]);
            }
            return matches;
        };
        const isContactLine = (line: string) => {
            return emailRegex.test(line) || /\b(?:https?:\/\/)?(?:www\.)?[A-Z0-9-]+(?:\.[A-Z0-9-]+)+/i.test(line) || /(?:\+\d{1,3}[\s.-]?\d{1,3}[\s.-]?\d{3,4}[\s.-]?\d{4}|0\d{1,2}[\s.-]?\d{3,4}[\s.-]?\d{4}|\d{3}[\s.-]\d{3,4}[\s.-]\d{4})/.test(line);
        };
        const normalizeEnglishName = (value: string) => {
            return (value || '').split(/\s+/).filter(Boolean).map((item: string) => {
                let word = item.charAt(0).toUpperCase() + item.slice(1).toLowerCase();
                if (word.toLowerCase() === 'kwon' || word.toLowerCase() === 'kwonn') word = 'Kwon';
                return word;
            }).join(' ');
        };
        const koreanNameFrom = (line: string) => {
            const match = (line || '').match(/((?:[가-힣]\s*){2,4})(?=\s*(?:개발|영업|기획|마케팅|전략|디자인|사업|팀|본부|센터|부서|사업부|부문|실|국|과|파트|랩|연구소|대표|이사|실장|팀장|부장|차장|과장|대리|주임|사원|책임|수석|선임|연구원|매니저|컨설턴트|디자이너|개발자|엔지니어|$))/);
            if (!match) return '';
            const name = match[1].replace(/\s+/g, '');
            if (/개발|영업|기획|마케팅|전략|디자인|사업|팀|본부|센터|부서|실|국|과|파트|랩|연구|책임|수석|선임|매니저/.test(name)) return '';
            return name.length >= 2 && name.length <= 4 ? name : '';
        };
        const englishNameFrom = (line: string) => {
            const match = (line || '').match(/\b([A-Z][A-Za-z]{1,24})\s+([A-Z][A-Za-z]{1,24})(?=\s+(?:Co-Founder|Founder|Owner|President|Vice|VP|Managing|General|Chief|CEO|CTO|CFO|COO|CPO|CMO|CIO|Principal|Partner|Senior|Junior|Staff|Associate|Consultant|Specialist|Designer|Developer|Researcher|Analyst|Architect|Product|Project|Sales|Marketing|Lead|Head|Manager|Director|Engineer|Officer|of\b))/);
            if (!match) return '';
            const name = normalizeEnglishName(match[0]);
            if (/Chief|Product|Project|Sales|Marketing|Senior|Junior|Staff|Associate|Consultant|Specialist|Designer|Developer|Researcher|Analyst|Architect|Manager|Director|Engineer|Officer|Founder|President|Lead|Head/i.test(name)) return '';
            return name;
        };
        const escapeRegex = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const extractName = (lines: string[]) => {
            for (const line of lines) {
                const labeled = line.match(nameLabelRegex);
                if (!labeled || companyRegex.test(line)) continue;
                const candidate = stripContact(labeled[1]);
                const koreanName = koreanNameFrom(candidate);
                if (koreanName) return koreanName;
                const englishName = candidate.match(/^([A-Z][A-Za-z]{1,24}(?:\s+[A-Z][A-Za-z]{1,24}){1,2})$/);
                if (englishName) return normalizeEnglishName(englishName[1]);
            }

            for (const line of lines) {
                if (companyRegex.test(line) || isContactLine(line) || addressStrongRegex.test(line)) continue;
                if (!roleRegex.test(line)) continue;
                const name = koreanNameFrom(line) || englishNameFrom(line);
                if (name) return name;
            }
            for (const line of lines) {
                if (isContactLine(line) || addressStrongRegex.test(line) || companyRegex.test(line)) continue;
                const koreanName = koreanNameFrom(line);
                if (koreanName && line.replace(/\s+/g, '').length <= koreanName.length + 2) return koreanName;
                const englishName = line.match(/^([A-Z][A-Za-z]{1,24}\s+[A-Z][A-Za-z]{1,24})$/);
                if (englishName) return normalizeEnglishName(englishName[1]);
            }
            return '';
        };
        const extractRole = (lines: string[], name: string) => {
            let department = '';
            let position = '';
            const nameVariants = name ? [name, /[가-힣]/.test(name) ? name.split('').join(' ') : ''].filter(Boolean) : [];
            for (const line of lines) {
                if (!roleRegex.test(line)) continue;
                if (companyRegex.test(line) || isContactLine(line) || addressStrongRegex.test(line)) continue;
                let work = line;
                for (const variant of nameVariants) {
                    work = work.replace(new RegExp(escapeRegex(variant), 'ig'), ' ');
                }
                if (!position) {
                    const match = work.match(positionRegex);
                    if (match) position = match[0];
                }
                if (!department) {
                    const english = work.match(/(?:Lead|Head|Manager|Director|Engineer|Officer|Consultant|Specialist)\s+(?:of|for)\s+([A-Z][A-Za-z0-9\s&/-]{2,70}?\s+(?:Team|Dept\.?|Department|Division|Office|Center|Centre|Lab|Laboratory|Group|Unit|Part|Squad|Chapter))\b/i)
                        || work.match(/\b([A-Z][A-Za-z0-9\s&/-]{2,70}?\s+(?:Team|Dept\.?|Department|Division|Office|Center|Centre|Lab|Laboratory|Group|Unit|Part|Squad|Chapter))\b/i);
                    if (english) {
                        department = cleanLine(english[1]);
                    } else {
                        const normalized = work.replace(/[^가-힣A-Za-z0-9\s/]/g, ' ');
                        const korean = normalized.match(/([가-힣A-Za-z0-9]{1,28}(?:팀|본부|센터|부서|사업부|부문|실|국|과|파트|랩|연구소))/);
                        if (korean) department = korean[1];
                    }
                }
                if (department && position) break;
            }
            return { department, position };
        };
        const normalizePhone = (value: string) => {
            return cleanLine(value).replace(/(?<=\d)\s+(?=\d)/g, '-').replace(/\s*[-.]\s*/g, '-');
        };
        const extractPhoneFields = (lines: string[]) => {
            let mobile = '';
            let phone = '';
            const fallbackNumbers: string[] = [];

            for (const line of lines) {
                const numbers = (line.match(phoneRegex) || []).map((item: string) => normalizePhone(item));
                if (numbers.length === 0) continue;

                const isFax = faxLabelRegex.test(line) && !mobileLabelRegex.test(line) && !phoneLabelRegex.test(line);
                const isMobile = mobileLabelRegex.test(line);
                const isPhone = phoneLabelRegex.test(line);

                for (const number of numbers) {
                    const normalized = number.replace(/\D/g, '');
                    const looksMobile = normalized.indexOf('01') === 0 || normalized.indexOf('8210') === 0;
                    if (isFax) continue;
                    if (looksMobile) {
                        if (!mobile) mobile = number;
                        continue;
                    }
                    if (!mobile && isMobile) {
                        mobile = number;
                    } else if (!phone && isPhone) {
                        phone = number;
                    } else {
                        fallbackNumbers.push(number);
                    }
                }
            }

            for (const number of fallbackNumbers) {
                const normalized = number.replace(/\D/g, '');
                if (!mobile && (normalized.indexOf('01') === 0 || normalized.indexOf('8210') === 0)) {
                    mobile = number;
                } else if (!phone && number !== mobile) {
                    phone = number;
                }
            }

            return { mobile, phone };
        };
        const extractAddress = (lines: string[]) => {
            const candidates: string[] = [];
            for (let index = 0; index < lines.length; index++) {
                if (!addressRegex.test(lines[index])) continue;
                for (const span of [1, 2, 3]) {
                    let candidate = lines.slice(index, index + span).join(' ');
                    candidate = candidate
                        .replace(/^.*?\b(?:Addr|Address|HQ|Headquarters|Office|Location|Located at)[,.]?\s*/i, '')
                        .replace(/^(?:주소|소재지|사업장|사무실|본사|지점)[:：.\s]*/, '');
                    candidate = candidate.split(companyRegex)[0];
                    candidate = candidate.split(/\b(?:주식회사|Season\s+Co|EASA|pee)\b/i)[0];
                    candidate = stripContact(candidate)
                        .replace(/\b(?:om|ZSy|aN|ee|oe|se|Ly|200)\b/ig, ' ');
                    candidate = cleanLine(candidate);
                    if (candidate.length >= 8) candidates.push(candidate);
                }
            }
            const score = (candidate: string) => {
                const patterns = [/자치시|특별시|광역시|Sejong|Seoul|Busan|\bCity\b/i, /대로|로\s|길\s|daero|Road|\bRd\.?\b|Street|\bSt\.?\b|\bAve\.?\b|Avenue/i, /프라자|빌딩|타워|센터|plaza|\bBldg\.?\b|Building|Suite|Floor/i, /Republic|Korea|USA/i, /한림|Hanlim/i];
                return patterns.filter((pattern: RegExp) => pattern.test(candidate)).length * 20 + candidate.length;
            };
            return candidates.sort((a: string, b: string) => score(b) - score(a))[0] || '';
        };

        const lines = raw.split(/[\n\r]+/)
            .map((line: string) => cleanLine(line))
            .filter((line: string) => line.length > 0);
        const joined = lines.join(' ');

        const emailMatch = joined.match(emailRegex);
        const websiteMatches = extractWebsites(joined);
        const companyLine = lines.map((line: string) => cleanCompany(line)).find((line: string) => line.length > 0) || '';
        const nameLine = extractName(lines);
        const role = extractRole(lines, nameLine);
        const contact = extractPhoneFields(lines);

        return {
            name: nameLine,
            company: companyLine,
            department: role.department,
            position: role.position,
            email: emailMatch ? emailMatch[0] : '',
            mobile: contact.mobile,
            phone: contact.phone,
            address: extractAddress(lines),
            website: websiteMatches.length > 0 ? websiteMatches[0] : ''
        };
    }

    private mergeFieldSets(items: any[]) {
        const keys = ['name', 'company', 'department', 'position', 'email', 'mobile', 'phone', 'address', 'website'];
        const merged: any = {};
        for (const key of keys) merged[key] = '';

        for (const item of items) {
            for (const key of keys) {
                if (!merged[key] && item[key]) merged[key] = item[key];
            }
        }

        return merged;
    }

    private applyParsedFields(parsed: any, source: string = 'photo') {
        for (const key of ['name', 'company', 'department', 'position', 'email', 'mobile', 'phone', 'address', 'website']) {
            if (parsed[key] && !this.form[key]) this.form[key] = parsed[key];
        }
        this.form.source = source;
    }

    public async enableEditMode() {
        if (this.formMode !== 'detail') return;
        this.formMode = 'edit';
        this.copiedField = '';
        await this.service.render();
    }

    public async cancelEditMode() {
        if (this.formMode !== 'edit') return;
        if (this.detailOriginal) {
            this.form = { ...this.emptyForm(), ...this.detailOriginal, tags: '' };
            this.captureSlots = this.captureSlotsFromCard(this.form);
        }
        this.formMode = 'detail';
        await this.service.render();
    }

    private duplicateContactValue(value: any) {
        const text = this.cleanValue(value);
        return text || '-';
    }

    private duplicateCardInfoBlock(title: string, card: any) {
        return [
            title,
            `이름: ${this.duplicateContactValue(card.name)}`,
            `이메일: ${this.duplicateContactValue(card.email)}`,
            `핸드폰: ${this.duplicateContactValue(card.mobile || card.phone)}`,
        ].join('\n');
    }

    private duplicateCardMessage(name: string, duplicates: any[], payload: any) {
        const first = duplicates[0] || {};
        const countText = duplicates.length > 1 ? `\n동일 이름 후보 ${duplicates.length}건 중 최근 수정된 명함에 적용합니다.` : '';
        const overwriteInfo = this.duplicateCardInfoBlock('덮어쓰기 대상', first);
        const createInfo = this.duplicateCardInfoBlock('새로 추가할 명함', payload);
        return `${name} 이름의 명함이 이미 있습니다.${countText}\n\n${overwriteInfo}\n\n${createInfo}\n\n같은 사람이라면 덮어쓰고, 다른 사람이면 새 명함으로 추가하세요.`;
    }

    private async resolveDuplicateSave(payload: any, data: any) {
        const duplicates = (data && data.duplicates) || [];
        if (this.formMode !== 'create' || duplicates.length === 0) {
            return { code: 409, data };
        }

        this.saving = false;
        await this.service.render();

        const overwrite = await this.service.modal.warning(
            this.duplicateCardMessage(payload.name, duplicates, payload),
            '덮어쓰기',
            '새로 추가',
            '같은 이름의 명함'
        );

        this.saving = true;
        await this.service.render();

        if (overwrite) {
            return await wiz.call('save', { ...payload, id: duplicates[0].id });
        }
        return await wiz.call('save', { ...payload, duplicate_action: 'create' });
    }

    public async save() {
        if (!this.form.name) {
            await this.service.modal.error('이름을 확인해주세요.');
            return;
        }

        this.saving = true;
        await this.service.render();
        const front = this.captureSlot('front');
        const back = this.captureSlot('back');
        const payload = {
            ...this.form,
            front_image: front.preview || this.form.front_image || '',
            back_image: back.preview || this.form.back_image || '',
            tags: ''
        };
        let { code, data } = await wiz.call('save', payload);
        if (code === 409) {
            const response = await this.resolveDuplicateSave(payload, data);
            code = response.code;
            data = response.data;
        }
        this.saving = false;

        if (code === 200) {
            if (this.formMode === 'edit') {
                const updated = data && data.card ? data.card : payload;
                this.form = { ...this.emptyForm(), ...updated, tags: '', source: updated.source || this.form.source || 'photo' };
                this.detailOriginal = { ...this.form };
                this.captureSlots = this.captureSlotsFromCard(this.form);
                this.formMode = 'detail';
            } else {
                await this.closeForm();
            }
            await this.load(this.search.page);
        } else {
            await this.service.modal.error((data && data.message) || '저장에 실패했습니다.');
        }
        await this.service.render();
    }

    public saveButtonLabel() {
        if (this.saving) return this.formMode === 'edit' ? '수정 중...' : '저장 중...';
        return this.formMode === 'edit' ? '수정 저장' : '저장';
    }

    public async remove(card: any) {
        const ok = await this.service.modal.error(`${card.name} 명함을 삭제하시겠습니까?`, '삭제', '취소');
        if (!ok) return;

        const { code, data } = await wiz.call('remove', { id: card.id });
        if (code === 200) {
            await this.load(this.search.page);
        } else {
            await this.service.modal.error(data.message || '삭제에 실패했습니다.');
        }
    }

    public primaryContact(card: any) {
        return card.mobile || card.phone || card.email || '-';
    }

    public roleLine(card: any) {
        return [card.position, card.department].filter((value: string) => !!value).join(' / ');
    }

    private cleanValue(value: any) {
        return String(value || '').trim();
    }

    private compactFields(fields: any[]) {
        return fields
            .map((field: any) => ({ ...field, value: this.cleanValue(field.value) }))
            .filter((field: any) => !!field.value);
    }

    public profileFields() {
        const role = [this.form.department, this.form.position].filter((value: string) => !!this.cleanValue(value)).join(' / ');
        return this.compactFields([
            { key: 'company', label: '회사', value: this.form.company },
            { key: 'role', label: '부서/직책', value: role },
        ]);
    }

    private websiteHref(value: any) {
        const text = this.cleanValue(value);
        if (!text) return '';
        if (/^https?:\/\//i.test(text)) return text;
        return `https://${text}`;
    }

    private telHref(value: any) {
        const text = this.cleanValue(value).replace(/[^\d+]/g, '');
        return text ? `tel:${text}` : '';
    }

    public contactFields() {
        const email = this.cleanValue(this.form.email);
        const mobile = this.cleanValue(this.form.mobile);
        const phone = this.cleanValue(this.form.phone);
        const website = this.cleanValue(this.form.website);
        return this.compactFields([
            { key: 'email', label: '이메일', value: email, href: email ? `mailto:${email}` : '', actionType: 'email', actionTitle: '메일 보내기' },
            { key: 'mobile', label: '휴대폰', value: mobile, href: this.telHref(mobile), actionType: 'phone', actionTitle: '휴대폰으로 전화' },
            { key: 'phone', label: '전화', value: phone, href: this.telHref(phone), actionType: 'phone', actionTitle: '전화 걸기' },
            { key: 'website', label: '웹사이트', value: website, href: this.websiteHref(website), actionType: 'external', actionTitle: '웹사이트 열기', target: '_blank', rel: 'noopener noreferrer' },
            { key: 'address', label: '주소', value: this.form.address },
        ]);
    }

    public memoFields() {
        return this.compactFields([
            { key: 'memo', label: '메모', value: this.form.memo },
        ]);
    }

    public hasReadableFields() {
        return this.profileFields().length + this.contactFields().length + this.memoFields().length > 0;
    }

    public detailInitials() {
        const text = this.cleanValue(this.form.name || this.form.company || '?');
        return text.slice(0, 2);
    }

    public async copyField(key: string, value: any) {
        const text = this.cleanValue(value);
        if (!text) return;

        let copied = false;
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(text);
                copied = true;
            }
        } catch (error) {
            copied = false;
        }

        if (!copied) copied = this.copyWithFallback(text);
        if (!copied) return;

        this.copiedField = key;
        await this.service.render();
        window.setTimeout(async () => {
            if (this.copiedField === key) {
                this.copiedField = '';
                await this.service.render();
            }
        }, 1100);
    }

    private copyWithFallback(text: string) {
        try {
            const textarea = document.createElement('textarea');
            textarea.value = text;
            textarea.setAttribute('readonly', '');
            textarea.style.position = 'fixed';
            textarea.style.opacity = '0';
            document.body.appendChild(textarea);
            textarea.select();
            const copied = document.execCommand('copy');
            document.body.removeChild(textarea);
            return copied;
        } catch (error) {
            return false;
        }
    }

    public sourceLabel(card: any) {
        if (card.source === 'photo-ai') return 'AI 분석';
        if (card.source === 'photo-server') return '서버 분석';
        if (card.source === 'photo-browser') return '사진 분석';
        return card.source && card.source.indexOf('photo') === 0 ? '사진' : '이전 등록';
    }

    public analysisClass() {
        if (this.analysis.status === 'done') return 'is-done';
        if (this.analysis.status === 'failed') return 'is-failed';
        if (this.analysis.status === 'analyzing') return 'is-active';
        return 'is-idle';
    }
}
