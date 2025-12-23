package dev.matheus.resource;

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

    @Inject
    ChatService chatService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createChat(@RestForm("file") FileUpload file) {
        LOG.infof("Creating new chat with file upload: fileName=%s, contentType=%s",
                  file != null ? file.fileName() : "null",
                  file != null ? file.contentType() : "null");

        if (!isValidFile(file)) {
            LOG.warnf("Invalid file type rejected: contentType=%s", file != null ? file.contentType() : "null");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Tipo de arquivo inválido. Apenas PDF e DOCX são permitidos."))
                    .build();
        }

        byte[] fileData;
        try {
            fileData = Files.readAllBytes(file.uploadedFile());
            LOG.debugf("File read successfully: size=%d bytes", fileData.length);
        } catch (IOException e) {
            LOG.errorf(e, "Failed to read uploaded file: %s", file.fileName());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erro ao processar o arquivo: " + e.getMessage()))
                    .build();
        }

        ChatResponse chat = chatService.createChat(fileData, file.fileName(), file.contentType());
        LOG.infof("Chat created successfully: chatId=%s, title=%s, fileName=%s",
                  chat.id(), chat.title(), file.fileName());
        return Response.status(Response.Status.CREATED).entity(chat).build();
    }


//    @GET
//    public List<ChatResponse> listChats() {
//        LOG.debug("Listing all chats");
//        List<ChatResponse> chats = chatService.listChats();
//        LOG.debugf("Found %d chats", chats.size());
//        return chats;
//    }

    @GET
    @Path("/{chatId}")
    public ChatResponse getChat(@PathParam("chatId") String chatId) {
        LOG.debugf("Getting chat: chatId=%s", chatId);
        Chat chat = chatService.getChat(chatId);
        LOG.debugf("Chat retrieved: chatId=%s", chatId);
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
        LOG.infof("Deleting chat: chatId=%s", chatId);
        chatService.deleteChat(chatId);
        LOG.infof("Chat deleted successfully: chatId=%s", chatId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{chatId}/messages")
    public List<ChatMessageResponse> getChatMessages(@PathParam("chatId") String chatId) {
        LOG.debugf("Getting messages for chat: chatId=%s", chatId);
        List<ChatMessageResponse> messages = chatService.getChatMessages(chatId);
        LOG.debugf("Found %d messages for chatId=%s", messages.size(), chatId);
        return messages;
    }


//    @POST
//    @Path("/{chatId}/document")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    public Response uploadDocument(@PathParam("chatId") String chatId,
//                                    @MultipartForm DocumentUploadForm form) {
//        try {
//            // Validar tipo de arquivo
//            String contentType = form.file.contentType();
//            if (!isValidFileType(contentType)) {
//                return Response.status(Response.Status.BAD_REQUEST)
//                        .entity(new ErrorResponse("Tipo de arquivo inválido. Apenas PDF e DOCX são permitidos."))
//                        .build();
//            }
//
//            // Ler bytes do arquivo
//            byte[] fileData = form.file.uploadedFile().readAllBytes();
//
//            DocumentFileResponse response = chatService.uploadDocument(
//                    chatId,
//                    form.file.fileName(),
//                    contentType,
//                    (long) fileData.length,
//                    fileData
//            );
//
//            return Response.status(Response.Status.CREATED).entity(response).build();
//        } catch (IOException e) {
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .entity(new ErrorResponse("Erro ao processar o arquivo: " + e.getMessage()))
//                    .build();
//        }
//    }

    @GET
    @Path("/{chatId}/document")
    public DocumentFileResponse getDocumentInfo(@PathParam("chatId") String chatId) {
        LOG.debugf("Getting document info for chat: chatId=%s", chatId);
        DocumentFileResponse doc = chatService.getDocument(chatId);
        LOG.debugf("Document info retrieved: chatId=%s, fileName=%s", chatId, doc.fileName());
        return doc;
    }

    @GET
    @Path("/{chatId}/document/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDocument(@PathParam("chatId") String chatId) {
        LOG.infof("Downloading document for chat: chatId=%s", chatId);
        DocumentFile file = chatService.getDocumentEntity(chatId);
        LOG.infof("Document downloaded: chatId=%s, fileName=%s, size=%d bytes",
                  chatId, file.fileName, file.fileSize);
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
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                        contentType.equals("application/msword")
        );
    }


}

