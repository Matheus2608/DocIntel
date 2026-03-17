import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useWebSocketChat } from '../useWebSocketChat';

// ---------------------------------------------------------------------------
// WebSocket mock
// ---------------------------------------------------------------------------

let mockWsInstance = null;

class MockWebSocket {
  constructor(url) {
    this.url = url;
    this.readyState = MockWebSocket.OPEN;
    this.onopen = null;
    this.onmessage = null;
    this.onclose = null;
    this.onerror = null;
    this.sentMessages = [];
    mockWsInstance = this;
  }

  send(data) {
    this.sentMessages.push(data);
  }

  close() {
    this.readyState = MockWebSocket.CLOSED;
    if (this.onclose) this.onclose({ code: 1000 });
  }
}

MockWebSocket.OPEN = 1;
MockWebSocket.CLOSED = 3;

// Stub global.fetch so loadPastMessages doesn't throw
const stubFetch = () => {
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    json: async () => [],
  });
};

const simulateOpen = () => {
  if (mockWsInstance?.onopen) {
    act(() => { mockWsInstance.onopen({}); });
  }
};

const simulateMessage = (data) => {
  if (mockWsInstance?.onmessage) {
    act(() => { mockWsInstance.onmessage({ data }); });
  }
};

const simulateCompletion = (messageId = 'msg-1', content = 'final content') => {
  const payload = JSON.stringify({ messageId, content });
  simulateMessage(payload);
};

// ---------------------------------------------------------------------------
// Setup / teardown
// ---------------------------------------------------------------------------

beforeEach(() => {
  mockWsInstance = null;
  global.WebSocket = MockWebSocket;
  stubFetch();
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('useWebSocketChat – token buffer queue', () => {

  /**
   * Test 1: Buffer accumulates tokens
   *
   * When multiple streaming chunks arrive rapidly, the hook must accumulate
   * them in a buffer ref (tokenBufferRef) and NOT immediately push each
   * individual chunk into the displayed message state.
   *
   * The current implementation calls continueStreaming() synchronously on
   * every WebSocket message which updates React state directly — so a
   * message built from 5 rapid chunks will trigger 5 state updates rather
   * than being held in a buffer.  This test confirms that the buffer ref is
   * populated and that not every arrival triggers a new render cycle.
   */
  it('accumulates incoming streaming tokens in a buffer without immediately flushing to state', () => {
    const { result } = renderHook(() =>
      useWebSocketChat('chat-1', 'ws://localhost/ws/chat-1')
    );

    simulateOpen();

    // Send a user message to start streaming mode
    act(() => { result.current.sendMessage('hello'); });

    // Deliver 5 rapid token chunks
    const tokens = ['Hel', 'lo ', 'wor', 'ld ', '!'];
    tokens.forEach(t => simulateMessage(t));

    // The hook must expose a tokenBufferRef whose current value holds the
    // tokens that have not yet been displayed.  The buffer should be
    // non-empty immediately after token arrival (before the timer fires).
    expect(result.current.tokenBufferRef).toBeDefined();
    expect(result.current.tokenBufferRef.current).toBeDefined();
    expect(Array.isArray(result.current.tokenBufferRef.current)).toBe(true);

    // The buffer must still contain items right after token arrival because
    // the timer has not fired yet (fake timers – no time has advanced).
    expect(result.current.tokenBufferRef.current.length).toBeGreaterThan(0);
  });

  /**
   * Test 2: Constant display rate via setInterval
   *
   * The display timer must fire every 30 ms and advance the visible text by
   * at most 3 characters per tick.  React state updates should happen on the
   * timer schedule, not on every individual WebSocket message event.
   *
   * The current implementation has no interval; every message triggers an
   * immediate state update.  This test asserts timer-gated behaviour.
   */
  it('flushes buffered tokens at a constant rate (every 30ms, 3 chars per tick)', () => {
    const { result } = renderHook(() =>
      useWebSocketChat('chat-2', 'ws://localhost/ws/chat-2')
    );

    simulateOpen();
    act(() => { result.current.sendMessage('hi'); });

    // Deliver a long streaming token in one shot
    simulateMessage('ABCDEFGHIJKLMNOPQRSTUVWXYZ');

    // Before any timer fires, the assistant message should be empty or absent
    // because buffering must hold the content back.
    const messagesBefore = result.current.messages;
    const assistantBefore = messagesBefore.find(m => m.role === 'assistant');
    // The displayed assistant text must NOT already equal the full token string
    expect(assistantBefore?.text ?? '').not.toBe('ABCDEFGHIJKLMNOPQRSTUVWXYZ');

    // Advance time by exactly one tick (30ms)
    act(() => { vi.advanceTimersByTime(30); });

    const messagesAfterOneTick = result.current.messages;
    const assistantAfterOneTick = messagesAfterOneTick.find(m => m.role === 'assistant');
    const textAfterOneTick = assistantAfterOneTick?.text ?? '';

    // After one 30ms tick, at most 3 characters should be visible
    expect(textAfterOneTick.length).toBeGreaterThan(0);
    expect(textAfterOneTick.length).toBeLessThanOrEqual(3);

    // Advance by more ticks and confirm text grows progressively
    act(() => { vi.advanceTimersByTime(90); }); // 3 more ticks
    const assistantAfterFourTicks = result.current.messages.find(m => m.role === 'assistant');
    expect((assistantAfterFourTicks?.text ?? '').length).toBeGreaterThan(textAfterOneTick.length);
  });

  /**
   * Test 3: Buffer drains completely after the completion JSON arrives
   *
   * When the server sends the JSON completion message, any tokens still
   * sitting in the buffer must all be displayed before the stream is marked
   * as finalized (currentStreamRef reset, isTyping false).
   *
   * The current implementation clears currentStreamRef on JSON receipt
   * without first draining any buffer, so remaining tokens would be lost.
   */
  it('drains the remaining buffer before finalising when the completion message arrives', () => {
    const { result } = renderHook(() =>
      useWebSocketChat('chat-3', 'ws://localhost/ws/chat-3')
    );

    simulateOpen();
    act(() => { result.current.sendMessage('question'); });

    // Streaming tokens arrive but the timer has not fired yet
    simulateMessage('Part1 ');
    simulateMessage('Part2 ');
    simulateMessage('Part3');

    // Completion arrives while buffer still holds content
    simulateCompletion('msg-42', 'Part1 Part2 Part3');

    // After completion the full text must be visible in the assistant message
    // (the buffer must have been flushed synchronously or the drain must
    // complete before the finalisation step).
    const assistantMsg = result.current.messages.find(m => m.role === 'assistant');
    expect(assistantMsg).toBeDefined();
    expect(assistantMsg.text).toBe('Part1 Part2 Part3');

    // The stream must be marked as finished (no active currentStreamRef)
    // and isTyping must be false.
    expect(result.current.isTyping).toBe(false);
  });

  /**
   * Test 4: Timer stops when not actively streaming
   *
   * The setInterval display timer should only run while streaming is active.
   * When no chat is selected, or after streaming completes and the buffer is
   * empty, no interval should be running.
   *
   * The current implementation has no interval at all, but once the feature
   * is implemented it must not leak timers.  This test verifies that after
   * completion the interval is cleared so that subsequent timer ticks do not
   * trigger additional state updates.
   */
  it('stops the display timer when streaming completes and the buffer is empty', () => {
    const setIntervalSpy = vi.spyOn(globalThis, 'setInterval');
    const clearIntervalSpy = vi.spyOn(globalThis, 'clearInterval');

    const { result } = renderHook(() =>
      useWebSocketChat('chat-4', 'ws://localhost/ws/chat-4')
    );

    simulateOpen();
    act(() => { result.current.sendMessage('test'); });

    // Token arrives — timer should start
    simulateMessage('token');

    // Timer must have been registered during streaming
    expect(setIntervalSpy).toHaveBeenCalledWith(expect.any(Function), 30);

    // Advance timers so buffer drains
    act(() => { vi.advanceTimersByTime(300); });

    // After the buffer is empty, the interval must be cleared
    expect(clearIntervalSpy).toHaveBeenCalled();
  });

});
