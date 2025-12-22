package dev.matheus.resource;

import dev.matheus.dto.ChatMessageResponse;
import dev.matheus.dto.ChatResponse;
import dev.matheus.dto.DocumentFileResponse;
import dev.matheus.entity.DocumentFile;
import dev.matheus.service.ChatService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Path("/api/chats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    @Inject
    ChatService chatService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createChat(@RestForm("file") FileUpload file) {
        if (!isValidFile(file)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Tipo de arquivo inválido. Apenas PDF e DOCX são permitidos."))
                    .build();
        }

        byte[] fileData;
        try (InputStream f = file.uploadedFile()){
            fileData = f.readAllBytes();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Erro ao processar o arquivo: " + e.getMessage()))
                    .build();
        }

        ChatResponse chat = chatService.createChat(fileData, file.fileName, file.contentType);
        return Response.status(Response.Status.CREATED).entity(chat).build();
    }


    @GET
    public List<ChatResponse> listChats() {
        return chatService.listChats();
    }

    @GET
    @Path("/{chatId}")
    public ChatResponse getChat(@PathParam("chatId") String chatId) {
        return chatService.getChat(chatId);
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

    @POST
    @Path("/{chatId}/messages")
    public Response addMessage(@PathParam("chatId") String chatId, MessageRequest request) {
        ChatMessageResponse message = chatService.addMessage(chatId, request.role(), request.content());
        return Response.status(Response.Status.CREATED).entity(message).build();
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
        return chatService.getDocument(chatId);
    }

    @GET
    @Path("/{chatId}/document/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDocument(@PathParam("chatId") String chatId) {
        DocumentFile file = chatService.getDocumentEntity(chatId);
        return Response.ok(file.fileData)
                .header("Content-Disposition", "attachment; filename=\"" + file.fileName + "\"")
                .header("Content-Type", file.fileType)
                .build();
    }

    public record FileUpload(String fileName, String contentType, InputStream uploadedFile) {}

    public record MessageRequest(String role, String content) {}

    public record ErrorResponse(String message) {}

    private boolean isValidFile(FileUpload file) {
        return file != null && isValidFileType(file.contentType);
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                        contentType.equals("application/msword")
        );
    }


}

