package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/env")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/env", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface EnvironmentEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CREATE, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "createEnvironmentV1")
    DetailedEnvironmentResponse post(@Valid EnvironmentRequest request);

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "getEnvironmentV1ByName")
    DetailedEnvironmentResponse getByName(@PathParam("name") String environmentName);

    @GET
    @Path("/crnByName/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET_CRN_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "getCrnByNameV1")
    EnvironmentCrnResponse getCrnByName(@PathParam("name") String environmentName);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironmentV1ByName")
    SimpleEnvironmentResponse deleteByName(@PathParam("name") String environmentName,
        @QueryParam("cascading") @DefaultValue("true") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("/name")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironmentsByName", httpMethod = "DELETE")
    SimpleEnvironmentResponses deleteMultipleByNames(Set<String> names,
        @QueryParam("cascading") @DefaultValue("true") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.EDIT_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "editEnvironmentV1")
    DetailedEnvironmentResponse editByName(@PathParam("name") String environmentName, @NotNull EnvironmentEditRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES, nickname = "listEnvironmentV1")
    SimpleEnvironmentResponses list();

    @PUT
    @Path("/name/{name}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_CREDENTIAL_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironmentV1")
    DetailedEnvironmentResponse changeCredentialByEnvironmentName(@PathParam("name") String environmentName, @Valid EnvironmentChangeCredentialRequest request);

    @PUT
    @Path("/name/{name}/change_telemetry_features")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_TELEMETRY_FEATURES_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "changeTelemetryFeaturesInEnvironmentV1ByName")
    DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentName(@PathParam("name") String environmentName, @Valid FeaturesRequest request);

    @POST
    @Path("/name/{name}/cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CLI_COMMAND, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "getCreateEnvironmentForCliByName")
    Object getCreateEnvironmentForCliByName(@PathParam("name") String environmentName);

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.GET_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "getEnvironmentV1ByCrn")
    DetailedEnvironmentResponse getByCrn(@PathParam("crn") String crn);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironmentV1ByCrn")
    SimpleEnvironmentResponse deleteByCrn(@PathParam("crn") String crn,
        @QueryParam("cascading") @DefaultValue("true") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("/crn")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.DELETE_MULTIPLE_BY_CRN, produces = MediaType.APPLICATION_JSON,
            notes = ENVIRONMENT_NOTES, nickname = "deleteEnvironmentsByCrn", httpMethod = "DELETE")
    SimpleEnvironmentResponses deleteMultipleByCrns(Set<String> crns,
        @QueryParam("cascading") @DefaultValue("true") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.EDIT_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "editEnvironmentV1ByCrn")
    DetailedEnvironmentResponse editByCrn(@PathParam("crn") String crn, @NotNull EnvironmentEditRequest request);

    @PUT
    @Path("/crn/{crn}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_CREDENTIAL_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "changeCredentialInEnvironmentV1ByCrn")
    DetailedEnvironmentResponse changeCredentialByEnvironmentCrn(@PathParam("crn") String crn, @Valid EnvironmentChangeCredentialRequest request);

    @PUT
    @Path("/crn/{crn}/change_telemetry_features")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CHANGE_TELEMETRY_FEATURES_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "changeTelemetryFeaturesInEnvironmentV1ByCrn")
    DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentCrn(@PathParam("crn") String crn, @Valid FeaturesRequest request);

    @POST
    @Path("/name/{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.START_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "startEnvironmentByNameV1")
    void postStartByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.START_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "startEnvironmentByCrnV1")
    void postStartByCrn(@PathParam("crn") String crn);

    @POST
    @Path("/name/{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.STOP_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "stopEnvironmentByNameV1")
    void postStopByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.STOP_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "stopEnvironmentByCrnV1")
    void postStopByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/crn/{crn}/verify_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.VERIFY_CREDENTIAL_BY_CRN, notes = ENVIRONMENT_NOTES,
            nickname = "verifyCredentialByEnvCrn", httpMethod = "GET")
    CredentialResponse verifyCredentialByEnvCrn(@PathParam("crn") String crn);

    @POST
    @Path("/crn/{crn}/cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CLI_COMMAND, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "getCreateEnvironmentForCliByCrn")
    Object getCreateEnvironmentForCliByCrn(@PathParam("crn") String crn);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.CLI_COMMAND, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
            nickname = "getCreateEnvironmentForCli")
    Object getCreateEnvironmentForCli(@NotNull @Valid EnvironmentRequest environmentRequest);

    @POST
    @Path("/crn/{crn}/update_config")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = EnvironmentOpDescription.UPDATE_CONFIG_BY_CRN, produces = MediaType.APPLICATION_JSON, notes = ENVIRONMENT_NOTES,
        nickname = "updateConfigsInEnvironmentByCrnV1")
    void updateConfigsInEnvironmentByCrn(@PathParam("crn") String crn);
}
