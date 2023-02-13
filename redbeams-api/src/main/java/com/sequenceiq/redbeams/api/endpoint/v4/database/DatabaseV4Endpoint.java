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

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RetryAndMetrics
@Path("/v4/databases")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "databases")
@SecurityScheme(type = SecuritySchemeType.APIKEY, name = RedbeamsApi.CRN_HEADER_API_KEY, in = SecuritySchemeIn.HEADER, paramName = "x-cdp-actor-crn")
public interface DatabaseV4Endpoint {

    @GET
    @Path("")
    @Operation(summary =  DatabaseOpDescription.LIST, description =  DatabaseNotes.LIST,
            operationId = "listDatabases")
    DatabaseV4Responses list(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotNull @Parameter(description = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn
    );

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary =  DatabaseOpDescription.REGISTER, description =  DatabaseNotes.REGISTER,
            operationId = "registerDatabase")
    DatabaseV4Response register(
        @Valid @Parameter(description = DatabaseParamDescriptions.DATABASE_REQUEST) DatabaseV4Request request
    );

    @GET
    @Path("/{crn}")
    @Operation(summary =  DatabaseOpDescription.GET_BY_CRN, description =  DatabaseNotes.GET_BY_CRN,
            operationId = "getDatabaseByCrn")
    DatabaseV4Response getByCrn(
        @ValidCrn(resource = CrnResourceDescriptor.DATABASE) @NotNull @Parameter(description = DatabaseParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @GET
    @Path("/name/{name}")
    @Operation(summary =  DatabaseOpDescription.GET_BY_NAME, description =  DatabaseNotes.GET_BY_NAME,
            operationId = "getDatabaseByName")
    DatabaseV4Response getByName(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotNull @Parameter(description = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @Parameter(description = DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("/{crn}")
    @Operation(summary =  DatabaseOpDescription.DELETE_BY_CRN, description =  DatabaseNotes.DELETE_BY_CRN,
            operationId = "deleteDatabaseByCrn")
    DatabaseV4Response deleteByCrn(
        @ValidCrn(resource = CrnResourceDescriptor.DATABASE) @NotNull @Parameter(description = DatabaseParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @DELETE
    @Path("/name/{name}")
    @Operation(summary =  DatabaseOpDescription.DELETE_BY_NAME, description =  DatabaseNotes.DELETE_BY_NAME,
            operationId = "deleteDatabaseByName")
    DatabaseV4Response deleteByName(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotNull @Parameter(description = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @Parameter(description = DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary =  DatabaseOpDescription.DELETE_MULTIPLE_BY_CRN, description =  DatabaseNotes.DELETE_MULTIPLE_BY_CRN,
            operationId ="deleteMultipleDatabasesByCrn")
    DatabaseV4Responses deleteMultiple(
        @Parameter(description = DatabaseParamDescriptions.CRNS) @ValidCrn(resource = CrnResourceDescriptor.DATABASE) Set<String> crns
    );

    @POST
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary =  DatabaseOpDescription.TEST_CONNECTION, description =  DatabaseNotes.TEST_CONNECTION,
            operationId ="testDatabaseConnection")
    DatabaseTestV4Response test(
        @Valid @Parameter(description = DatabaseParamDescriptions.DATABASE_TEST_REQUEST) DatabaseTestV4Request databaseTestV4Request
    );
}
