package dev.matheus.resource;

import dev.matheus.entity.RetrievalInfo;
import dev.matheus.service.RetrievalInfoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Path("/api/retrieve")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RagRetrievalResource {

    private static final Logger LOG = Logger.getLogger(RagRetrievalResource.class);

    @Inject
    public RetrievalInfoService retrievalInfoService;

    @GET
    @Path("/{messageId}")
    public RetrievalInfo retrieve(@PathParam("messageId") String messageId) {
        LOG.infof("Received retrieval request: messageId=%s", messageId);
        RetrievalInfo info = retrievalInfoService.getRetrievalInfoByChatMessageId(messageId);
        LOG.debugf("Retrieval info returned successfully: messageId=%s", messageId);
        return info;
    }

}
