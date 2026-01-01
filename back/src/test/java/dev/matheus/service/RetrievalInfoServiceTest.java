package dev.matheus.service;

import dev.matheus.dto.ChatMessageResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@QuarkusTest
class RetrievalInfoServiceTest {

    @InjectMock
    ChatService chatService;

    @Inject
    RetrievalInfoService retrievalInfoService;

    private String testChatId = "test-chat-id";

    @Test
    void shouldRetryIfThereIsNoUserMessage() {
        when(chatService.getLastUserMessage(testChatId))
                .thenThrow(new NotFoundException());

        assertThrows(NotFoundException.class,
                () -> retrievalInfoService.getMessageId(testChatId));

        // Verifica que foi chamado 3 vezes (1 tentativa + 2 retries)
        verify(chatService, times(3))
                .getLastUserMessage(testChatId);
    }

    @Test
    void shouldRetrieveUserMessageIfExists() throws InterruptedException {
        String expectedMessageId = "message-123";
        when(chatService.getLastUserMessage(testChatId))
                .thenReturn(new ChatMessageResponse(expectedMessageId, null, null, null));

        String actualMessageId = retrievalInfoService.getMessageId(testChatId);

        assertThat(actualMessageId).isNotNull().isEqualTo(expectedMessageId);

        // Verifica que foi chamado apenas 1 vez
        verify(chatService, times(1))
                .getLastUserMessage(testChatId);
    }


}