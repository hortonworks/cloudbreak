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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/{workspaceId}/databases")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/databases", description = ControllerDescription.DATABASES_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "listDatabasesByWorkspace")
    DatabaseV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") @DefaultValue("false") Boolean attachGlobal);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "createDatabaseInWorkspace")
    DatabaseV4Response create(@PathParam("workspaceId") Long workspaceId, @Valid DatabaseV4Request request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "getDatabaseInWorkspace")
    DatabaseV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "deleteDatabaseInWorkspace")
    DatabaseV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "deleteDatabasesInWorkspace")
    DatabaseV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.GET_REQUEST, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
            nickname = "getDatabaseRequestFromNameInWorkspace")
    DatabaseV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

}
