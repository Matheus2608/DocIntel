package dev.matheus.resource;

import dev.matheus.dto.ChatListResponse;
import dev.matheus.dto.ChatMessageResponse;
import dev.matheus.dto.ChatResponse;
import dev.matheus.dto.DocumentFileResponse;
import dev.matheus.entity.Chat;
import dev.matheus.entity.DocumentFile;
import dev.matheus.service.ChatService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Path("/api/chats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private static final Logger LOG = Logger.getLogger(ChatResource.class);

    private static final List<String> ALLOWED_FILE_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain"
    );

    @Inject
    ChatService chatService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createChat(@RestForm("file") FileUpload file) {
        LOG.infof("Creating chat: fileName=%s", file != null ? file.fileName() : "null");

        if (!isValidFile(file)) {
            LOG.warnf("Invalid file type: %s", file != null ? file.contentType() : "null");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Tipo de arquivo inválido. Apenas PDF e DOCX são permitidos."))
                    .build();
        }

        byte[] fileData;
        try {
            fileData = Files.readAllBytes(file.uploadedFile());
        } catch (IOException e) {
            LOG.errorf(e, "Failed to read file: %s", file.fileName());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erro ao processar o arquivo: " + e.getMessage()))
                    .build();
        }

        ChatResponse chat = chatService.createChat(fileData, file.fileName(), file.contentType());
        LOG.infof("Chat created: chatId=%s", chat.id());
        return Response.status(Response.Status.CREATED).entity(chat).build();
    }

    @GET
    public List<ChatListResponse> listChats() {
        return chatService.listChats();
    }

    @GET
    @Path("/{chatId}")
    public ChatResponse getChat(@PathParam("chatId") String chatId) {
        Chat chat = chatService.getChat(chatId);
        return mapToChatResponse(chat);
    }

    private ChatResponse mapToChatResponse(Chat chat) {
        return new ChatResponse(
                chat.id,
                chat.title,
                chat.createdAt,
                chat.updatedAt,
                chat.documentFile != null
        );
    }

    @DELETE
    @Path("/{chatId}")
    public Response deleteChat(@PathParam("chatId") String chatId) {
        chatService.deleteChat(chatId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{chatId}/messages")
    public List<ChatMessageResponse> getChatMessages(@PathParam("chatId") String chatId) {
        return chatService.getChatMessages(chatId);
    }

    @GET
    @Path("/{chatId}/document")
    public DocumentFileResponse getDocumentInfo(@PathParam("chatId") String chatId) {
        return chatService.getDocument(chatId);
    }

    @GET
    @Path("/{chatId}/document/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDocument(@PathParam("chatId") String chatId) {
        LOG.infof("Downloading document: chatId=%s", chatId);
        DocumentFile file = chatService.getDocumentEntity(chatId);
        return Response.ok(file.fileData)
                .header("Content-Disposition", "attachment; filename=\"" + file.fileName + "\"")
                .header("Content-Type", file.fileType)
                .build();
    }

    public record ErrorResponse(String message) {}

    private boolean isValidFile(FileUpload file) {
        return file != null && isValidFileType(file.contentType());
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && ALLOWED_FILE_TYPES.contains(contentType);
    }
}

