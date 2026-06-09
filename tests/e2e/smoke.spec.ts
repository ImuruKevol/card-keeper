import { expect, test } from '@playwright/test';

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'https://bus.sub.nanoha.kr';
const hostname = new URL(baseURL).hostname;
const projectName = process.env.WIZ_PROJECT || 'main';

test.beforeEach(async ({ context }) => {
    await context.addCookies([
        {
            name: 'season-wiz-project',
            value: projectName,
            domain: hostname,
            path: '/',
        },
        {
            name: 'season-wiz-devmode',
            value: 'true',
            domain: hostname,
            path: '/',
        },
    ]);
});

test('로그인 화면을 표시한다', async ({ page }) => {
    await page.goto('/access');

    await expect(page.getByRole('heading', { name: '명함장' })).toBeVisible();
    await expect(page.getByRole('button', { name: '로그인' }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: '가입 신청' }).first()).toBeVisible();
});

test('PWA manifest를 제공한다', async ({ request }) => {
    const response = await request.get('/manifest.json');

    expect(response.ok()).toBeTruthy();
    const manifest = await response.json();
    expect(manifest.name).toBe('명함장');
    expect(manifest.start_url).toBe('/cards');
    expect(manifest.display).toBe('standalone');
    expect(manifest.icons.length).toBeGreaterThan(0);
});

test('비로그인 명함 화면 접근은 로그인 화면으로 이동한다', async ({ page }) => {
    await page.goto('/cards', { waitUntil: 'domcontentloaded' });

    await expect(page).toHaveURL(/\/access(?:\?|$)/);
});
