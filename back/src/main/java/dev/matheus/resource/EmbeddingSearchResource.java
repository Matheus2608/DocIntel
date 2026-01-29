package dev.matheus.resource;

import dev.matheus.dto.*;
import dev.matheus.service.EmbeddingSearchService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * REST API for embedding search and management
 */
@Path("/api/embeddings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmbeddingSearchResource {

    private static final Logger LOG = Logger.getLogger(EmbeddingSearchResource.class);

    @Inject
    EmbeddingSearchService embeddingSearchService;

    /**
     * Search embeddings by text query
     * POST /api/embeddings/search
     */
    @POST
    @Path("/search")
    public Response search(@Valid EmbeddingSearchRequest request) {
        LOG.infof("Search request: query=%s", request.query());
        
        try {
            EmbeddingSearchResponse response = embeddingSearchService.search(request);
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error searching embeddings");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error searching embeddings: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Add a new embedding entry
     * POST /api/embeddings
     */
    @POST
    public Response addEntry(@Valid EmbeddingAddRequest request) {
        LOG.infof("Add entry request: fileName=%s", request.fileName());
        
        try {
            EmbeddingAddResponse response = embeddingSearchService.addEntry(request);
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error adding embedding entry");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error adding embedding entry: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get a single embedding entry by ID
     * GET /api/embeddings/{id}
     */
    @GET
    @Path("/{id}")
    public Response getEntry(@PathParam("id") @NotBlank String entryId) {
        LOG.infof("Get entry request: id=%s", entryId);
        
        try {
            EmbeddingEntryResponse response = embeddingSearchService.getEntry(entryId);
            return Response.ok(response).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Embedding entry not found"))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error getting embedding entry");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error getting embedding entry: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Update an existing embedding entry
     * PUT /api/embeddings/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateEntry(@PathParam("id") @NotBlank String entryId,
                                @Valid EmbeddingUpdateRequest request) {
        LOG.infof("Update entry request: id=%s", entryId);
        
        // Validate that the path param matches the request body
        if (!entryId.equals(request.entryId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Entry ID in path does not match request body"))
                    .build();
        }
        
        if (!request.hasUpdates()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("At least one field must be provided for update"))
                    .build();
        }
        
        try {
            EmbeddingUpdateResponse response = embeddingSearchService.updateEntry(request);
            return Response.ok(response).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Embedding entry not found"))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error updating embedding entry");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error updating embedding entry: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Delete an embedding entry
     * DELETE /api/embeddings/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteEntry(@PathParam("id") @NotBlank String entryId) {
        LOG.infof("Delete entry request: id=%s", entryId);
        
        try {
            embeddingSearchService.deleteEntry(entryId);
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Embedding entry not found"))
                    .build();
        } catch (Exception e) {
            LOG.errorf(e, "Error deleting embedding entry");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error deleting embedding entry: " + e.getMessage()))
                    .build();
        }
    }
}
