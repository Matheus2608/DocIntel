---
name: webapp-testing
description: Write and run Playwright end-to-end tests for the DocIntel React frontend. Use this skill whenever the user wants to validate frontend behavior, test UI interactions, add e2e tests, check if the app works after a change, or finalize a TDD cycle with browser-level validation. Trigger on phrases like "test the app", "validate the UI", "write e2e tests", "run the frontend tests", "check if the frontend works", "test this feature in the browser", or after any frontend RED-GREEN-REFACTOR cycle completes. When in doubt, use this skill — browser tests catch regressions that unit tests miss.
---

# Web App Testing — DocIntel

Write and run Playwright e2e tests to validate the DocIntel frontend. This is the final gate in the TDD workflow: after unit and integration tests pass on the backend, browser tests confirm the full stack works as the user actually experiences it.

## Step 1: Determine the scenario

Before doing anything else, check which situation you're in:

```bash
# Are there already e2e tests?
ls front/e2e/ 2>/dev/null && echo "TESTS EXIST" || echo "NO TESTS YET"

# Is Playwright configured?
ls front/playwright.config.ts 2>/dev/null && echo "CONFIGURED" || echo "NEEDS SETUP"
```

- **Tests exist → go to Step 3 (run them)**
- **Tests don't exist → go to Step 2 (read components, then write them)**

## Step 2: Read the components before writing tests

This step is easy to skip and costly to skip. Selectors written without reading the real components will be wrong. Always read the relevant component files before writing any `page.locator()` or `getByRole()` calls.

```bash
# Identify which components are relevant to what you're testing
ls front/src/components/
ls front/src/hooks/
```

Read the component files to understand:
- What HTML elements and class names are actually rendered
- How state flows (props, hooks, context)
- What user interactions trigger what behavior

Then write tests that target elements the way Playwright recommends: prefer `getByRole()`, `getByText()`, `getByLabel()` over class selectors. Add `data-testid` attributes to the component only if there's no better semantic anchor.

## Step 3: Setup (first time only)

If `front/playwright.config.ts` doesn't exist:

```bash
cd front
npm install -D @playwright/test
npx playwright install chromium
mkdir -p e2e
```

Create `front/playwright.config.ts`:

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 1,
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
```

Add to `front/package.json` scripts:
```json
"test:e2e": "playwright test",
"test:e2e:ui": "playwright test --ui"
```

Check that both servers are running before proceeding:
```bash
curl -sf http://localhost:8080/api/chats > /dev/null || echo "⚠ Backend not running — start with: cd back && ./mvnw quarkus:dev"
curl -sf http://localhost:5173 > /dev/null             || echo "⚠ Frontend not running — start with: cd front && npm run dev"
```

## Step 4: What to test

Cover these flows, in priority order. Each should be a separate spec file in `front/e2e/`.

### Chat lifecycle (`e2e/chat.spec.ts`)
The sidebar manages chats. Test: create a chat, verify it appears, delete it, verify it disappears. Also test the empty state.

### Document upload (`e2e/upload.spec.ts`)
The `Upload.jsx` component handles drag-and-drop. Test: open the upload panel, drop a PDF (use `page.setInputFiles()` for headless file input), verify success feedback. Test that an unsupported file type shows an error.

### Message streaming (`e2e/messaging.spec.ts`)
The `InputMessage.jsx` → WebSocket → `ChatMessage.jsx` flow. Type a message, submit, verify the user bubble appears instantly, then wait for the AI response to finish streaming. AI responses can take 10–30 seconds — use `waitForSelector` with a generous timeout:
```typescript
await page.waitForSelector('.ai-message', { timeout: 30_000 });
```
The exact selector depends on what you find in `ChatMessage.jsx`.

### RAG modal (`e2e/rag-modal.spec.ts`)
After an AI response arrives (from above), click the sources button. The `RagModal.jsx` should open showing chunks with embedding and semantic scores. Test open, inspect scores, close.

### Embedding search (`e2e/embedding-search.spec.ts`)
The `EmbeddingSearch.tsx` panel: enter a query, submit, verify results appear in `EmbeddingSearchResults.tsx`. Test that the edit and add modals open/close correctly.

### Theme toggle (`e2e/theme.spec.ts`)
Find the toggle in `Header.jsx`. Click it, verify the CSS class or variable changes on `document.body`. Verify persistence via `localStorage`:
```typescript
const theme = await page.evaluate(() => localStorage.getItem('theme'));
```

## Step 5: Run and report

```bash
cd front

# All tests
npm run test:e2e

# Specific file
npx playwright test e2e/chat.spec.ts

# With browser visible (useful for debugging)
npx playwright test --headed

# Show report from last run
npx playwright show-report
```

Report back with:
- Tests run: X passed, Y failed
- Which flows are covered vs missing
- For failures: paste the error + screenshot path from `playwright-report/`
- Verdict: **ready to commit** or **fix first**

If a test fails, distinguish between:
- **Real bug** → go back and fix the implementation, re-run
- **Fragile selector** → fix the test (use a more stable locator from the component source)
- **Timing issue** → increase the timeout, don't retry blindly
