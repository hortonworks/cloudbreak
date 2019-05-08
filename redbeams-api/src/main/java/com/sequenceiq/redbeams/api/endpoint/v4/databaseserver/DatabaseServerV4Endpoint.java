package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver;

import javax.validation.Valid;
//import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
//import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
//
import com.sequenceiq.cloudbreak.doc.ContentType;
//import com.sequenceiq.redbeams.api.endpoint.v4.common.EnvironmentIds;
//import com.sequenceiq.redbeams.api.endpoint.v4.database.requests.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
//import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.doc.ControllerDescriptions;
import com.sequenceiq.redbeams.doc.Notes;
import com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseServerOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

//import java.util.Set;

@Path("/v4/{workspaceId}/databaseservers")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/databaseservers", description = ControllerDescriptions.DATABASE_SERVER_V4_DESCRIPTION, protocols = "http,https")
public interface DatabaseServerV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "listDatabasesServersByWorkspace")
    DatabaseServerV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environmentId") String environmentId,
        @QueryParam("attachGlobal") @DefaultValue("false") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "getDatabaseServerInWorkspace")
    DatabaseServerV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.REGISTER_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "registerDatabaseServerInWorkspace")
    DatabaseServerV4Response register(@PathParam("workspaceId") Long workspaceId, @Valid DatabaseServerV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "deleteDatabaseServerInWorkspace")
    DatabaseServerV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

//    @DELETE
//    @Path("")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
//            nickname = "deleteDatabasesInWorkspace")
//    DatabaseV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);
//
//    @GET
//    @Path("{name}/request")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = DatabaseOpDescription.GET_REQUEST_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
//            nickname = "getDatabaseRequestFromNameInWorkspace")
//    DatabaseV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
//
//    @POST
//    @Path("test")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = DatabaseOpDescription.POST_CONNECTION_TEST, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
//            nickname = "testDatabaseConnectionInWorkspace")
//    DatabaseTestV4Response test(@PathParam("workspaceId") Long workspaceId, @Valid DatabaseTestV4Request databaseTestV4Request);
//
//    @PUT
//    @Path("{name}/attach")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = DatabaseOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
//            nickname = "attachDatabaseToEnvironments")
//    DatabaseV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
//        @Valid @NotNull EnvironmentNames environmentNames);
//
//    @PUT
//    @Path("{name}/detach")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = DatabaseOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.DATABASE_NOTES,
//            nickname = "detachDatabaseFromEnvironments")
//    DatabaseV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
//        @Valid @NotNull EnvironmentNames environmentNames);
}
