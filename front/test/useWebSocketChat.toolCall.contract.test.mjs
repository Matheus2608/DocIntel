import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const hookPath = new URL('../src/hooks/useWebSocketChat.js', import.meta.url);

test('useWebSocketChat should handle tool_call step start/end events', async () => {
  const source = await readFile(hookPath, 'utf8');

  assert.match(
    source,
    /type\s*===\s*['"]tool_call['"]|type\s*:\s*['"]tool_call['"]/,
    'Expected hook to recognize tool_call websocket events'
  );

  assert.match(
    source,
    /buildStep\s*=|function\s+buildStep\s*\(/,
    'Expected hook to define buildStep helper for step objects'
  );

  assert.match(
    source,
    /updateSteps\s*=|function\s+updateSteps\s*\(/,
    'Expected hook to define updateSteps helper for tool_call events'
  );

  assert.match(
    source,
    /status\s*:\s*['"]running['"].*status\s*:\s*['"]done['"]|['"]start['"].*['"]end['"]/s,
    'Expected hook to map tool_call start/end into running/done assistant steps'
  );
});
