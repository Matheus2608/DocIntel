package dev.matheus.resource;

import dev.matheus.entity.RetrievalInfo;
import dev.matheus.service.RetrievalInfoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/retrieve")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RagRetrievalResource {

    @Inject
    public RetrievalInfoService retrievalInfoService;

    @GET
    @Path("/{messageId}")
    public RetrievalInfo retrieve(@PathParam("messageId") String messageId) {
        try {
            return retrievalInfoService.getRetrievalInfoByChatMessageId(messageId);
        } catch (NotFoundException ex) {
            retrievalInfoService.retrieveAndSaveInfo(messageId);
            return retrievalInfoService.getRetrievalInfoByChatMessageId(messageId);
        }
    }

}
