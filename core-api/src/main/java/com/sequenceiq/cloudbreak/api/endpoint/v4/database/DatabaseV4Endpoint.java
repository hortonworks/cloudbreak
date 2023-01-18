package com.sequenceiq.cloudbreak.api.endpoint.v4.database;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.DatabaseOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @deprecated obsolete API, should not be used, redbeams is the new service for database operations.
 * Decided not to delete in order to keep API  backward compatibility, added only restriction to call it only with
 * internal actor
 */
@Deprecated
@RetryAndMetrics
@Path("/v4/{workspaceId}/databases")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/{workspaceId}/databases", description = ControllerDescription.DATABASES_V4_DESCRIPTION)
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.LIST, description = Notes.DATABASE_NOTES,
            operationId = "listDatabasesByWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") @DefaultValue("false") Boolean attachGlobal);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.CREATE, description = Notes.DATABASE_NOTES,
            operationId = "createDatabaseInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid DatabaseV4Request request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.GET_BY_NAME, description = Notes.DATABASE_NOTES,
            operationId = "getDatabaseInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.DELETE_BY_NAME, description = Notes.DATABASE_NOTES,
            operationId = "deleteDatabaseInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME, description = Notes.DATABASE_NOTES,
            operationId = "deleteDatabasesInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.GET_REQUEST, description = Notes.DATABASE_NOTES,
            operationId = "getDatabaseRequestFromNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
