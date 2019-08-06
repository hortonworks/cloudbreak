package com.sequenceiq.redbeams.api.endpoint.v4.database;

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
import com.sequenceiq.redbeams.api.RedbeamsApi;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseTestV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseTestV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Responses;
import com.sequenceiq.redbeams.doc.Notes.DatabaseNotes;
import com.sequenceiq.redbeams.doc.OperationDescriptions.DatabaseOpDescription;
import com.sequenceiq.redbeams.doc.ParamDescriptions.DatabaseParamDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

import org.springframework.validation.annotation.Validated;

@Path("/v4/databases")
@Produces(MediaType.APPLICATION_JSON)
@Api(tags = { "databases" },
    protocols = "http,https",
    authorizations = { @Authorization(value = RedbeamsApi.CRN_HEADER_API_KEY) })
@Validated
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @ApiOperation(value = DatabaseOpDescription.LIST, notes = DatabaseNotes.LIST,
            nickname = "listDatabases")
    DatabaseV4Responses list(
        @ValidCrn @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn
    );

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.REGISTER, notes = DatabaseNotes.REGISTER,
            nickname = "registerDatabase")
    DatabaseV4Response register(
        @Valid @ApiParam(DatabaseParamDescriptions.DATABASE_REQUEST) DatabaseV4Request request
    );

    @GET
    @Path("/{name}")
    @ApiOperation(value = DatabaseOpDescription.GET_BY_NAME, notes = DatabaseNotes.GET_BY_NAME,
            nickname = "getDatabase")
    DatabaseV4Response get(
        @ValidCrn @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @ApiParam(DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("/{name}")
    @ApiOperation(value = DatabaseOpDescription.DELETE_BY_NAME, notes = DatabaseNotes.DELETE_BY_NAME,
            nickname = "deleteDatabase")
    DatabaseV4Response delete(
        @ValidCrn @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @ApiParam(DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.DELETE_MULTIPLE_BY_NAME, notes = DatabaseNotes.DELETE_MULTIPLE_BY_NAME,
            nickname = "deleteMultipleDatabases")
    DatabaseV4Responses deleteMultiple(
        @ValidCrn @NotNull @ApiParam(value = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @ApiParam(DatabaseParamDescriptions.NAMES) Set<String> names
    );

    @POST
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DatabaseOpDescription.TEST_CONNECTION, notes = DatabaseNotes.TEST_CONNECTION,
            nickname = "testDatabaseConnection")
    DatabaseTestV4Response test(
        @Valid @ApiParam(DatabaseParamDescriptions.DATABASE_TEST_REQUEST) DatabaseTestV4Request databaseTestV4Request
    );
}
