import { test, expect } from '@playwright/test';

test.describe('tool_call step rendering', () => {
  test('shows running and done steps during streamed assistant reply', async ({ page }) => {
    await page.addInitScript(() => {
      class MockWebSocket {
        static CONNECTING = 0;
        static OPEN = 1;
        static CLOSING = 2;
        static CLOSED = 3;

        readyState = MockWebSocket.CONNECTING;
        onopen = null;
        onmessage = null;
        onclose = null;
        onerror = null;

        constructor(_url) {
          setTimeout(() => {
            this.readyState = MockWebSocket.OPEN;
            if (this.onopen) this.onopen(new Event('open'));
          }, 20);
        }

        send(_data) {
          setTimeout(() => {
            if (this.onmessage) {
              this.onmessage({
                data: JSON.stringify({ messageId: 'msg-user-1', content: 'ok' }),
              });
            }
          }, 25);

          setTimeout(() => {
            if (this.onmessage) {
              this.onmessage({
                data: JSON.stringify({
                  type: 'tool_call',
                  tool: 'searchByHypotheticalQuestions',
                  status: 'start',
                }),
              });
            }
          }, 45);

          setTimeout(() => {
            if (this.onmessage) {
              this.onmessage({ data: 'Primeiro chunk. ' });
            }
          }, 70);

          setTimeout(() => {
            if (this.onmessage) {
              this.onmessage({ data: 'Segundo chunk.' });
            }
          }, 90);

          setTimeout(() => {
            if (this.onmessage) {
              this.onmessage({
                data: JSON.stringify({
                  type: 'tool_call',
                  tool: 'searchByHypotheticalQuestions',
                  status: 'end',
                }),
              });
            }
          }, 700);
        }

        close() {
          this.readyState = MockWebSocket.CLOSED;
          if (this.onclose) this.onclose(new Event('close'));
        }

        addEventListener() {}
        removeEventListener() {}
      }

      // @ts-ignore
      window.WebSocket = MockWebSocket;
    });

    await page.route('**/api/chats', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { id: 'chat-1', fileName: 'Tool Call Doc' },
        ]),
      });
    });

    await page.route('**/api/chats/chat-1/messages', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      });
    });

    await page.goto('/');

    await page.getByText('Tool Call Doc').click();

    const input = page.getByPlaceholder('Digite sua pergunta aqui...');
    await expect(input).toBeEnabled();
    await input.fill('Pergunta de teste');
    await input.press('Enter');

    await expect(page.getByText('Pergunta de teste')).toBeVisible();
    await expect(page.getByText('Primeiro chunk. Segundo chunk.')).toBeVisible();

    const stepsToggle = page.getByRole('button', { name: /Passos do agente \(\d+\)/ });
    await expect(stepsToggle).toBeVisible();
    await stepsToggle.click();

    const stepItems = page.locator('li', { hasText: 'Busca por questões hipotéticas' });
    await expect(stepItems.first()).toBeVisible();
    await expect(stepItems.filter({ hasText: '⟳' }).first()).toBeVisible();
    await expect(stepItems.filter({ hasText: '✓' }).first()).toBeVisible({ timeout: 10000 });
  });
});
