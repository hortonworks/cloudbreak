package com.sequenceiq.redbeams.api.endpoint.v4.database;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(summary = DatabaseOpDescription.LIST, description = DatabaseNotes.LIST,
            operationId = "listDatabases",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Responses list(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotNull @Parameter(description = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn
    );

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.REGISTER, description = DatabaseNotes.REGISTER,
            operationId = "registerDatabase",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response register(
        @Valid @Parameter(description = DatabaseParamDescriptions.DATABASE_REQUEST) DatabaseV4Request request
    );

    @GET
    @Path("/{crn}")
    @Operation(summary = DatabaseOpDescription.GET_BY_CRN, description = DatabaseNotes.GET_BY_CRN,
            operationId = "getDatabaseByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response getByCrn(
        @ValidCrn(resource = CrnResourceDescriptor.DATABASE) @NotNull @Parameter(description = DatabaseParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @GET
    @Path("/name/{name}")
    @Operation(summary = DatabaseOpDescription.GET_BY_NAME, description = DatabaseNotes.GET_BY_NAME,
            operationId = "getDatabaseByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response getByName(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotNull @Parameter(description = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @Parameter(description = DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("/{crn}")
    @Operation(summary = DatabaseOpDescription.DELETE_BY_CRN, description = DatabaseNotes.DELETE_BY_CRN,
            operationId = "deleteDatabaseByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response deleteByCrn(
        @ValidCrn(resource = CrnResourceDescriptor.DATABASE) @NotNull @Parameter(description = DatabaseParamDescriptions.CRN) @PathParam("crn") String crn
    );

    @DELETE
    @Path("/name/{name}")
    @Operation(summary = DatabaseOpDescription.DELETE_BY_NAME, description = DatabaseNotes.DELETE_BY_NAME,
            operationId = "deleteDatabaseByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Response deleteByName(
        @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotNull @Parameter(description = DatabaseParamDescriptions.ENVIRONMENT_CRN, required = true)
        @QueryParam("environmentCrn") String environmentCrn,
        @Parameter(description = DatabaseParamDescriptions.NAME) @PathParam("name") String name
    );

    @DELETE
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.DELETE_MULTIPLE_BY_CRN, description = DatabaseNotes.DELETE_MULTIPLE_BY_CRN,
            operationId = "deleteMultipleDatabasesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseV4Responses deleteMultiple(
        @Parameter(description = DatabaseParamDescriptions.CRNS) @ValidCrn(resource = CrnResourceDescriptor.DATABASE) Set<String> crns
    );

    @POST
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = DatabaseOpDescription.TEST_CONNECTION, description = DatabaseNotes.TEST_CONNECTION,
            operationId = "testDatabaseConnection",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DatabaseTestV4Response test(
        @Valid @Parameter(description = DatabaseParamDescriptions.DATABASE_TEST_REQUEST) DatabaseTestV4Request databaseTestV4Request
    );
}
