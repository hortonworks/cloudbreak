package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.redbeams.api.endpoint.v4.database.request.CreateDatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.CreateDatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
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
    DatabaseServerV4Responses list(@NotNull @QueryParam("environmentCrn") String environmentCrn);

    @GET
    @Path("{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "getDatabaseServerByCrn")
    DatabaseServerV4Response getByCrn(@PathParam("crn") String crn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "getDatabaseServerByName")
    DatabaseServerV4Response getByName(@NotNull @QueryParam("environmentCrn") String environmentCrn, @PathParam("name") String name);

    @POST
    @Path("managed")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "createDatabaseServer")
    DatabaseServerStatusV4Response create(@Valid AllocateDatabaseServerV4Request request);

    @GET
    @Path("managed/status/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.GET_STATUS_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "getDatabaseServerStatusByCrn")
    DatabaseServerStatusV4Response getStatusOfManagedDatabaseServerByCrn(@PathParam("crn") String crn);

    @GET
    @Path("managed/status/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.GET_STATUS_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "getDatabaseServerStatusByName")
    DatabaseServerStatusV4Response getStatusOfManagedDatabaseServerByName(
            @NotNull @QueryParam("environmentCrn") String environmentCrn,
            @NotNull @PathParam("name") String name
    );

    @DELETE
    @Path("managed/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.TERMINATE, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "terminateManagedDatabaseServer")
    DatabaseServerTerminationOutcomeV4Response terminate(@PathParam("crn") String crn);

    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.REGISTER, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "registerDatabaseServer")
    DatabaseServerV4Response register(@Valid DatabaseServerV4Request request);

    @DELETE
    @Path("{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
        nickname = "deleteDatabaseServerByCrn")
    DatabaseServerV4Response deleteByCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "deleteDatabaseServerByName")
    DatabaseServerV4Response deleteByName(@NotNull @QueryParam("environmentCrn") String environmentCrn, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.DATABASE_SERVER_NOTES,
            nickname = "deleteMultipleDatabaseServersByCrn")
    DatabaseServerV4Responses deleteMultiple(Set<String> crns);

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
            nickname = "createDatabaseOnServer")
    CreateDatabaseV4Response createDatabase(@Valid CreateDatabaseV4Request request);
}
