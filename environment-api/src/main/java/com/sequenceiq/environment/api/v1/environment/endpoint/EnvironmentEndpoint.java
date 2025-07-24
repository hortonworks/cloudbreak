package com.sequenceiq.environment.api.v1.environment.endpoint;

import static com.sequenceiq.environment.api.doc.environment.EnvironmentDescription.ENVIRONMENT_NOTES;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.telemetry.request.FeaturesRequest;
import com.sequenceiq.common.api.type.DataHubStartAction;
import com.sequenceiq.environment.api.doc.environment.EnvironmentOpDescription;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentCloudStorageValidationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentLoadBalancerUpdateRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.UpdateAzureResourceEncryptionParametersRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.environment.api.v1.environment.model.response.OutboundTypeValidationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponses;
import com.sequenceiq.environment.api.v1.environment.model.response.SupportedOperatingSystemResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/v1/env")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/env")
public interface EnvironmentEndpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CREATE, description = ENVIRONMENT_NOTES, operationId = "createEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse post(@Valid EnvironmentRequest request);

    @GET
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.GET_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "getEnvironmentV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse getByName(@PathParam("name") String environmentName);

    @GET
    @Path("/xp/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.GET_XP_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "getAttachedExperiencesByEnvironmentName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Map<String, Set<String>> getAttachedExperiencesByEnvironmentName(@PathParam("name") String name);

    @GET
    @Path("/crnByName/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.GET_CRN_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "getCrnByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EnvironmentCrnResponse getCrnByName(@PathParam("name") String environmentName);

    @DELETE
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.DELETE_BY_NAME,
            description = ENVIRONMENT_NOTES, operationId = "deleteEnvironmentV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleEnvironmentResponse deleteByName(@PathParam("name") String environmentName,
        @QueryParam("cascading") @DefaultValue("false") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("/name")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.DELETE_MULTIPLE_BY_NAME,
            description = ENVIRONMENT_NOTES, operationId = "deleteEnvironmentsByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleEnvironmentResponses deleteMultipleByNames(Set<String> names,
        @QueryParam("cascading") @DefaultValue("false") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("/name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.EDIT_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "editEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse editByName(@PathParam("name") String environmentName, @NotNull @Valid EnvironmentEditRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = EnvironmentOpDescription.LIST,
            description = ENVIRONMENT_NOTES + """
                remoteEnvironmentCrn is optional filter for those environment which are connected" +
                to the specified remote environment, if not provided, all environments" +
                in the account will be listed.
                """,
            operationId = "listEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true)
    )
    SimpleEnvironmentResponses list(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("remoteEnvironmentCrn") String remoteEnvironmentCrn
    );

    @GET
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.INTERNAL_LIST, description = ENVIRONMENT_NOTES,
            operationId = "internalListEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleEnvironmentResponses listInternal(@QueryParam("accountId") String accountId);

    @PUT
    @Path("/name/{name}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CHANGE_CREDENTIAL_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "changeCredentialInEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse changeCredentialByEnvironmentName(@PathParam("name") String environmentName, @Valid EnvironmentChangeCredentialRequest request);

    @PUT
    @Path("/name/{name}/update_azure_encryption_resources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPDATE_AZURE_ENCRYPTION_RESOURCES_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "UpdateAzureResourceEncryptionParametersV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse updateAzureResourceEncryptionParametersByEnvironmentName(@PathParam("name") String environmentName,
            @Valid UpdateAzureResourceEncryptionParametersRequest request);

    @PUT
    @Path("/name/{name}/change_telemetry_features")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CHANGE_TELEMETRY_FEATURES_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "changeTelemetryFeaturesInEnvironmentV1ByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentName(@PathParam("name") String environmentName, @Valid FeaturesRequest request);

    @POST
    @Path("/name/{name}/cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CLI_COMMAND, description = ENVIRONMENT_NOTES,
            operationId = "getCreateEnvironmentForCliByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getCreateEnvironmentForCliByName(@PathParam("name") String environmentName);

    @GET
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.GET_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "getEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse getByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @GET
    @Path("/xp/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.GET_XP_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "getAttachedExperiencesByEnvironmentCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Map<String, Set<String>> getAttachedExperiencesByEnvironmentCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @DELETE
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.DELETE_BY_CRN,
            description = ENVIRONMENT_NOTES, operationId = "deleteEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleEnvironmentResponse deleteByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
        @QueryParam("cascading") @DefaultValue("false") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @DELETE
    @Path("/crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.DELETE_MULTIPLE_BY_CRN,
            description = ENVIRONMENT_NOTES, operationId = "deleteEnvironmentsByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SimpleEnvironmentResponses deleteMultipleByCrns(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) Set<String> crns,
        @QueryParam("cascading") @DefaultValue("false") boolean cascading,
        @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("/crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.EDIT_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "editEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse editByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @NotNull @Valid EnvironmentEditRequest request);

    @PUT
    @Path("/crn/{crn}/change_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CHANGE_CREDENTIAL_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "changeCredentialInEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse changeCredentialByEnvironmentCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid EnvironmentChangeCredentialRequest request);

    @PUT
    @Path("/crn/{crn}/update_azure_encryption_resources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPDATE_AZURE_ENCRYPTION_RESOURCES_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "UpdateAzureResourceEncryptionParametersV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse updateAzureResourceEncryptionParametersByEnvironmentCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid UpdateAzureResourceEncryptionParametersRequest request);

    @PUT
    @Path("/crn/{crn}/change_telemetry_features")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CHANGE_TELEMETRY_FEATURES_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "changeTelemetryFeaturesInEnvironmentV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetailedEnvironmentResponse changeTelemetryFeaturesByEnvironmentCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @Valid FeaturesRequest request);

    @POST
    @Path("/name/{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.START_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "startEnvironmentByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier postStartByName(@PathParam("name") String name,
            @QueryParam("dataHubStartAction") @DefaultValue("START_ALL") DataHubStartAction dataHubStartAction);

    @POST
    @Path("/crn/{crn}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.START_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "startEnvironmentByCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier postStartByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @QueryParam("dataHubStartAction") @DefaultValue("START_ALL") DataHubStartAction dataHubStartAction);

    @POST
    @Path("/name/{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.STOP_BY_NAME, description = ENVIRONMENT_NOTES,
            operationId = "stopEnvironmentByNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier postStopByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.STOP_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "stopEnvironmentByCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier postStopByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @GET
    @Path("/crn/{crn}/verify_credential")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.VERIFY_CREDENTIAL_BY_CRN, description = ENVIRONMENT_NOTES,
            operationId = "verifyCredentialByEnvCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CredentialResponse verifyCredentialByEnvCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @POST
    @Path("/crn/{crn}/cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CLI_COMMAND, description = ENVIRONMENT_NOTES,
            operationId = "getCreateEnvironmentForCliByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getCreateEnvironmentForCliByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @POST
    @Path("cli_create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.CLI_COMMAND, description = ENVIRONMENT_NOTES,
            operationId = "getCreateEnvironmentForCli",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Object getCreateEnvironmentForCli(@NotNull @Valid EnvironmentRequest environmentRequest);

    @POST
    @Path("/crn/{crn}/update_config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPDATE_CONFIG_BY_CRN, description = ENVIRONMENT_NOTES,
        operationId = "updateConfigsInEnvironmentByCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateConfigsInEnvironmentByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @PUT
    @Path("/name/{name}/update_load_balancers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPDATE_LOAD_BALANCERS_BY_ENV_NAME, description = ENVIRONMENT_NOTES,
        operationId = "updateEnvironmentLoadBalancersByNameV11",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateEnvironmentLoadBalancersByName(@PathParam("name") String envName, @NotNull EnvironmentLoadBalancerUpdateRequest request);

    @PUT
    @Path("/crn/{crn}/update_load_balancers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPDATE_LOAD_BALANCERS_BY_ENV_CRN, description = ENVIRONMENT_NOTES,
        operationId = "updateEnvironmentLoadBalancersByCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateEnvironmentLoadBalancersByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
            @NotNull EnvironmentLoadBalancerUpdateRequest request);

    @GET
    @Path("/progress/resource/crn/{resourceCrn}/last")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.GET_LAST_FLOW_PROGRESS, description = ENVIRONMENT_NOTES,
            operationId = "getEnvironmentLastFlowLogProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowProgressResponse getLastFlowLogProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @GET
    @Path("/progress/resource/crn/{resourceCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.LIST_FLOW_PROGRESS, description = ENVIRONMENT_NOTES,
            operationId = "getEnvironmentFlowLogsProgressByResourceCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@PathParam("resourceCrn") String resourceCrn);

    @POST
    @Path("/validate_cloud_storage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.VALIDATE_CLOUD_STORAGE, description = ENVIRONMENT_NOTES,
            operationId = "validateCloudStorage",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ObjectStorageValidateResponse validateCloudStorage(@Valid EnvironmentCloudStorageValidationRequest environmentCloudStorageValidationRequest);

    @PUT
    @Path("/name/{name}/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPGRADE_CCM, description = ENVIRONMENT_NOTES,
            operationId = "upgradeCcmByEnvironmentNameV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier upgradeCcmByName(@PathParam("name") String name);

    @PUT
    @Path("/crn/{crn}/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.UPGRADE_CCM, description = ENVIRONMENT_NOTES,
            operationId = "upgradeCcmByEnvironmentCrnV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier upgradeCcmByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @PUT
    @Path("/name/{name}/vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.VERTICAL_SCALING, description = ENVIRONMENT_NOTES,
            operationId = "verticalScalingByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier verticalScalingByName(@PathParam("name") String name, @Valid VerticalScaleRequest updateRequest);

    @PUT
    @Path("/crn/{crn}/vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.VERTICAL_SCALING, description = ENVIRONMENT_NOTES,
            operationId = "verticalScalingByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier verticalScalingByCrn(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn,
        @Valid VerticalScaleRequest updateRequest);

    @GET
    @Path("/crn/{crn}/upgrade_ccm_available")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = EnvironmentOpDescription.UPGRADE_CCM_AVAILABLE, description = ENVIRONMENT_NOTES,
            operationId = "isUpgradeCcmAvailableV1ByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    boolean isUpgradeCcmAvailable(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @GET
    @Path("/crn/{crn}/validate_outbound_types")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = EnvironmentOpDescription.UPGRADE_DEFAULT_OUTBOUND_AVAILABLE, description = ENVIRONMENT_NOTES,
            operationId = "validateOutboundTypesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OutboundTypeValidationResponse validateOutboundTypes(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("crn") String crn);

    @GET
    @Path("/os/supported")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.LIST_SUPPORTED_OS, description = ENVIRONMENT_NOTES, operationId = "listSupportedOperatingSystemV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SupportedOperatingSystemResponse listSupportedOperatingSystem(@QueryParam("cloudPlatform") String cloudPlatform);

    @POST
    @Path("get_certificate_status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = EnvironmentOpDescription.LIST_CERTIFICATE_STATUS, description = ENVIRONMENT_NOTES,
            operationId = "listDatabaseServersCertificateStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    EnvironmentDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            @Valid @NotNull @Parameter(description = ENVIRONMENT_NOTES)
            EnvironmentDatabaseServerCertificateStatusV4Request request
    );

}
