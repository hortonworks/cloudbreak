package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.doc.ControllerDescriptions;
import com.sequenceiq.redbeams.doc.Notes;
import com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseServerOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

//import java.util.Set;

@Path("/v4/databaseservers")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/databaseservers", description = ControllerDescriptions.DATABASE_SERVER_V4_DESCRIPTION, protocols = "http,https")
public interface DatabaseServerV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "listDatabaseServers")
    DatabaseServerV4Responses list(@NotNull @QueryParam("environmentId") String environmentId,
        @QueryParam("attachGlobal") @DefaultValue("false") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "getDatabaseServer")
    DatabaseServerV4Response get(@NotNull @QueryParam("environmentId") String environmentId, @PathParam("name") String name);

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.REGISTER, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "registerDatabaseServer")
    DatabaseServerV4Response register(@Valid DatabaseServerV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "deleteDatabaseServer")
    DatabaseServerV4Response delete(@NotNull @QueryParam("environmentId") String environmentId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "deleteMultipleDatabaseServers")
    DatabaseServerV4Responses deleteMultiple(@NotNull @QueryParam("environmentId") String environmentId, Set<String> names);

//    @GET
//    @Path("{name}/request")
//    @Produces(MediaType.APPLICATION_JSON)
//    @ApiOperation(value = DatabaseOpDescription.GET_REQUEST_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_NOTES,
//            nickname = "getDatabaseRequestFromNameInWorkspace")
//    DatabaseV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
//

    @POST
    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.TEST_CONNECTION, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "testDatabaseServerConnection")
    DatabaseServerTestV4Response test(@Valid DatabaseServerTestV4Request request);

    @POST
    @Path("createDatabase")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.CREATE_DATABASE, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "createDatabase")
    CreateDatabaseV4Response createDatabase(@Valid CreateDatabaseV4Request request);
}
