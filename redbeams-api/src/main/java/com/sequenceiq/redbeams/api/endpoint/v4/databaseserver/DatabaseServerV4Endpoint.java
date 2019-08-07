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

import com.sequenceiq.cloudbreak.validation.ValidCrn;
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
import com.sequenceiq.redbeams.doc.Notes.DatabaseServerNotes;
import com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseServerOpDescription;
import com.sequenceiq.redbeams.doc.ParamDescriptions.DatabaseServerParamDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.validation.annotation.Validated;

@Path("/v4/databaseservers")
@Produces(MediaType.APPLICATION_JSON)
@Api(tags = {"database servers"},
        protocols = "http,https")
@Validated
public interface DatabaseServerV4Endpoint {

    @GET
    @Path("")
    @ApiOperation(value = DatabaseServerOpDescription.LIST, notes = DatabaseServerNotes.LIST,
            nickname = "listDatabaseServers")
    DatabaseServerV4Responses list(
            @ValidCrn @NotNull @ApiParam(value = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn
    );

    @GET
    @Path("{crn}")
    @ApiOperation(value = DatabaseServerOpDescription.GET_BY_CRN, notes = DatabaseServerNotes.GET_BY_CRN,
            nickname = "getDatabaseServerByCrn")
    DatabaseServerV4Response getByCrn(
            @ValidCrn @ApiParam(DatabaseServerParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @GET
    @Path("name/{name}")
    @ApiOperation(value = DatabaseServerOpDescription.GET_BY_NAME, notes = DatabaseServerNotes.GET_BY_NAME,
            nickname = "getDatabaseServerByName")
    DatabaseServerV4Response getByName(
            @ValidCrn @NotNull @ApiParam(value = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn,
            @ApiParam(DatabaseServerParamDescriptions.NAME) @PathParam("name") String name
    );

    @POST
    @Path("managed")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.CREATE, notes = DatabaseServerNotes.CREATE,
            nickname = "createDatabaseServer")
    DatabaseServerStatusV4Response create(
            @Valid @ApiParam(DatabaseServerParamDescriptions.ALLOCATE_DATABASE_SERVER_REQUEST) AllocateDatabaseServerV4Request request
    );

    @GET
    @Path("managed/status/{crn}")
    @ApiOperation(value = DatabaseServerOpDescription.GET_STATUS_BY_CRN, notes = DatabaseServerNotes.GET_STATUS_BY_CRN,
            nickname = "getDatabaseServerStatusByCrn")
    DatabaseServerStatusV4Response getStatusOfManagedDatabaseServerByCrn(
            @ValidCrn @ApiParam(DatabaseServerParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @GET
    @Path("managed/status/name/{name}")
    @ApiOperation(value = DatabaseServerOpDescription.GET_STATUS_BY_NAME, notes = DatabaseServerNotes.GET_STATUS_BY_NAME,
            nickname = "getDatabaseServerStatusByName")
    DatabaseServerStatusV4Response getStatusOfManagedDatabaseServerByName(
            @ValidCrn @NotNull @ApiParam(value = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn,
            @NotNull @ApiParam(DatabaseServerParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("managed/{crn}")
    @ApiOperation(value = DatabaseServerOpDescription.TERMINATE, notes = DatabaseServerNotes.TERMINATE,
            nickname = "terminateManagedDatabaseServer")
    DatabaseServerTerminationOutcomeV4Response terminate(
            @ValidCrn @ApiParam(DatabaseServerParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.REGISTER, notes = DatabaseServerNotes.REGISTER,
            nickname = "registerDatabaseServer")
    DatabaseServerV4Response register(
            @Valid @ApiParam(DatabaseServerParamDescriptions.DATABASE_SERVER_REQUEST) DatabaseServerV4Request request
    );

    @DELETE
    @Path("{crn}")
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_BY_CRN, notes = DatabaseServerNotes.DELETE_BY_CRN,
            nickname = "deleteDatabaseServerByCrn")
    DatabaseServerV4Response deleteByCrn(
            @ValidCrn @ApiParam(DatabaseServerParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @DELETE
    @Path("/name/{name}")
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_BY_NAME, notes = DatabaseServerNotes.DELETE_BY_NAME,
            nickname = "deleteDatabaseServerByName")
    DatabaseServerV4Response deleteByName(
            @ValidCrn @NotNull @ApiParam(value = DatabaseServerParamDescriptions.ENVIRONMENT_CRN, required = true)
            @QueryParam("environmentCrn") String environmentCrn,
            @ApiParam(DatabaseServerParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.DELETE_MULTIPLE_BY_CRN, notes = DatabaseServerNotes.DELETE_MULTIPLE_BY_CRN,
            nickname = "deleteMultipleDatabaseServersByCrn")
    DatabaseServerV4Responses deleteMultiple(
            @ApiParam(DatabaseServerParamDescriptions.CRNS) Set<@ValidCrn String> crns
    );

    @POST
    @Path("test")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.TEST_CONNECTION, notes = DatabaseServerNotes.TEST_CONNECTION,
            nickname = "testDatabaseServerConnection")
    DatabaseServerTestV4Response test(
            @Valid @ApiParam(DatabaseServerParamDescriptions.DATABASE_SERVER_TEST_REQUEST) DatabaseServerTestV4Request request
    );

    @POST
    @Path("createDatabase")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseServerOpDescription.CREATE_DATABASE, notes = DatabaseServerNotes.CREATE_DATABASE,
            nickname = "createDatabaseOnServer")
    CreateDatabaseV4Response createDatabase(
            @Valid @ApiParam(DatabaseServerParamDescriptions.CREATE_DATABASE_REQUEST) CreateDatabaseV4Request request
    );
}
