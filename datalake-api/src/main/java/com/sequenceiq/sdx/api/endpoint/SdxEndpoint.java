package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.UPDATE_SALT;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHANGE_IMAGE_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GENERATE_IMAGE_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ROTATE_CERTIFICATES;

import java.util.List;
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

import org.springframework.validation.annotation.Validated;

import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SdxBackupLocationValidationRequest;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxCustomClusterRequest;
import com.sequenceiq.sdx.api.model.SdxDefaultTemplateResponse;
import com.sequenceiq.sdx.api.model.SdxGenerateImageCatalogResponse;
import com.sequenceiq.sdx.api.model.SdxRecommendationResponse;
import com.sequenceiq.sdx.api.model.SdxRefreshDatahubResponse;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxStopValidationResponse;
import com.sequenceiq.sdx.api.model.SdxSyncComponentVersionsFromCmResponse;
import com.sequenceiq.sdx.api.model.SdxValidateCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/sdx")
public interface SdxEndpoint {

    @POST
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create SDX cluster", operationId ="createSdx")
    SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength @PathParam("name") String name, @Valid SdxClusterRequest createSdxClusterRequest);

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list SDX clusters", operationId ="listSdx")
    List<SdxClusterResponse> list(@QueryParam("envName") String envName, @DefaultValue("false") @QueryParam("includeDetached") boolean includeDetached);

    @GET
    @Path("list/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list SDX clusters internally", operationId ="listSdxInternal")
    List<SdxClusterResponse> internalList(@AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get SDX cluster", operationId ="getSdx")
    SdxClusterResponse get(@PathParam("name") String name);

    @GET
    @Path("/crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get SDX cluster by crn", operationId ="getSdxByCrn")
    SdxClusterResponse getByCrn(@PathParam("clusterCrn") @ValidCrn(resource = CrnResourceDescriptor.DATALAKE) String clusterCrn);

    @GET
    @Path("/envcrn/{envCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get SDX cluster by environment crn", operationId ="getSdxByEnvCrn")
    List<SdxClusterResponse> getByEnvCrn(@PathParam("envCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn);

    @GET
    @Path("{name}/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get SDX cluster detail", operationId ="getSdxDetail")
    SdxClusterDetailResponse getDetail(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("/crn/{clusterCrn}/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get SDX cluster detail by crn", operationId ="getSdxDetailByCrn")
    SdxClusterDetailResponse getDetailByCrn(@PathParam("clusterCrn") String clusterCrn, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "delete SDX cluster", operationId ="deleteSdx")
    FlowIdentifier delete(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("{name}/resize")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Re-size SDX cluster", operationId ="resizeSdx")
    SdxClusterResponse resize(@PathParam("name") String name, @Valid SdxClusterResizeRequest resizeSdxClusterRequest);

    @POST
    @Path("{datalakeName}/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Restart and reload all configurations of the data hub by name", operationId ="refreshDatahubs")
    SdxRefreshDatahubResponse refreshDataHubs(@PathParam("datalakeName") String name, @QueryParam("datahubName") String datahubName);

    @DELETE
    @Path("/crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "delete SDX cluster by crn", operationId ="deleteSdxByCrn")
    FlowIdentifier deleteByCrn(@PathParam("clusterCrn") String clusterCrn, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("{name}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "repairs an sdxNode in the specified hostgroup", operationId ="repairSdxNode")
    FlowIdentifier repairCluster(@PathParam("name") String name, SdxRepairRequest clusterRepairRequest);

    @POST
    @Path("/crn/{crn}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "repairs an sdxNode in the specified hostgroup by crn", operationId ="repairSdxNodeByCrn")
    FlowIdentifier repairClusterByCrn(@PathParam("crn") String crn, SdxRepairRequest clusterRepairRequest);

    @POST
    @Path("/crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "renew certificate on SDX cluster by crn", operationId ="renewCertificateOnSdxByCrn")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation", content = @Content(schema = @Schema(implementation = FlowIdentifier.class))) })
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);

    @POST
    @Path("{name}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "sync SDX cluster by name", operationId ="syncSdx")
    void sync(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "sync SDX cluster by crn", operationId ="syncSdxByCrn")
    void syncByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "retry sdx", operationId ="retrySdx")
    FlowIdentifier retry(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "retry sdx by crn", operationId ="retrySdxByCrn")
    FlowIdentifier retryByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "start sdx", operationId ="startSdxByName")
    FlowIdentifier startByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "start sdx by crn", operationId ="startSdxByCrn")
    FlowIdentifier startByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "stop sdx", operationId ="stopSdxByName")
    FlowIdentifier stopByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "stop sdx by crn", operationId ="stopSdxByCrn")
    FlowIdentifier stopByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{crn}/rotate_salt_password")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "rotate SaltStack user password", operationId ="rotateSaltPasswordSdxByCrn")
    FlowIdentifier rotateSaltPasswordByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("crn/{crn}/salt_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_SALT, operationId ="updateSaltSdxByCrn")
    FlowIdentifier updateSaltByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATALAKE) @PathParam("crn") String crn);

    @GET
    @Path("/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list datalake versions", operationId ="versions")
    List<String> versions(@QueryParam("cloudPlatform") String cloudPlatform);

    @GET
    @Path("/advertisedruntimes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list advertised datalake versions", operationId ="advertisedruntimes")
    List<AdvertisedRuntime> advertisedRuntimes(@QueryParam("cloudPlatform") String cloudPlatform);

    @POST
    @Path("/envcrn/{envCrn}/ranger_cloud_identity_mapping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set ranger cloud identity mapping", operationId ="setRangerCloudIdentityMapping")
    RangerCloudIdentitySyncStatus setRangerCloudIdentityMapping(@PathParam("envCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn, @NotNull @Valid SetRangerCloudIdentityMappingRequest request);

    @GET
    @Path("/envcrn/{envCrn}/ranger_cloud_identity_sync_status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get status of a ranger cloud identity sync", operationId ="getRangerCloudIdentitySyncStatus")
    RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(@PathParam("envCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn, @QueryParam("commandId") long commandId);

    @PUT
    @Path("{name}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_CERTIFICATES, operationId ="rotateAutoTlsCertificatesByName")
    FlowIdentifier rotateAutoTlsCertificatesByName(@PathParam("name") String name, @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("crn/{crn}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_CERTIFICATES, operationId ="rotateAutoTlsCertificatesByCrn")
    FlowIdentifier rotateAutoTlsCertificatesByCrn(@PathParam("crn") String crn, @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("/crn/{crn}/enable_ranger_raz")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set the rangerRazEnabled flag of the cluster if Raz is installed manually", operationId ="enableRangerRazByCrn")
    void enableRangerRazByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("/name/{name}/enable_ranger_raz")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set the rangerRazEnabled flag of the cluster if Raz is installed manually", operationId ="enableRangerRazByName")
    void enableRangerRazByName(@PathParam("name") String name);

    @POST
    @Path("{name}/custom_image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create custom SDX cluster", operationId ="createCustomSdx")
    SdxClusterResponse create(@PathParam("name") String name, @Valid SdxCustomClusterRequest createSdxClusterRequest);

    @POST
    @Path("validate_cloud_storage/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validate cloud storage", operationId ="validateCloudStorage")
    ObjectStorageValidateResponse validateCloudStorage(@ValidStackNameFormat @ValidStackNameLength @PathParam("name") String clusterName, @Valid SdxValidateCloudStorageRequest sdxValidateCloudStorageRequest);

    @POST
    @Path("validate_cloud_backup_storage")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validate cloud backup storage", operationId ="validateCloudBackupStorage")
    ValidationResult validateBackupStorage(@RequestObject @Valid SdxBackupLocationValidationRequest sdxBackupLocationValidationRequest);

    @PUT
    @Path("{name}/change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHANGE_IMAGE_CATALOG, operationId ="changeImageCatalog")
    void changeImageCatalog(@PathParam("name") String name, @Valid @NotNull SdxChangeImageCatalogRequest changeImageCatalogRequest);

    @POST
    @Path("{name}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "syncs CM and parcel versions from CM and updates SDX cluster version", operationId ="syncCmOnDatalakeCluster")
    SdxSyncComponentVersionsFromCmResponse syncComponentVersionsFromCmByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "syncs CM and parcel versions from CM and updates SDX cluster version", operationId ="syncCmOnDatalakeClusterByCrn")
    SdxSyncComponentVersionsFromCmResponse syncComponentVersionsFromCmByCrn(@PathParam("crn") String crn);

    @GET
    @Path("instance_group_names")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gather available instance group names by SDX cluster attributes", operationId ="getInstanceGroupNamesBySdxDetails")
    Set<String> getInstanceGroupNamesBySdxDetails(@QueryParam("clusterShape") SdxClusterShape clusterShape, @QueryParam("runtimeVersion") String runtimeVersion, @QueryParam("cloudPlatform") String cloudPlatform);

    @GET
    @Path("name/{name}/generate_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GENERATE_IMAGE_CATALOG, operationId ="generateImageCatalog")
    SdxGenerateImageCatalogResponse generateImageCatalog(@PathParam("name") String name);

    @GET
    @Path("default_template")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets the default template for the given datalake shape, cloud platform and runtime version", operationId ="getDefaultTemplate")
    SdxDefaultTemplateResponse getDefaultTemplate(@QueryParam("clusterShape") SdxClusterShape clusterShape, @QueryParam("runtimeVersion") String runtimeVersion, @QueryParam("cloudPlatform") String cloudPlatform);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Gets default and recommended instance types for the given datalake shape, cloud platform and runtime version", operationId ="getRecommendation")
    SdxRecommendationResponse getRecommendation(@NotNull(message = "The 'credentialCrn' query parameter must be specified.") @QueryParam("credentialCrn") String credentialCrn, @NotNull(message = "The 'clusterShape' query parameter must be specified.") @QueryParam("clusterShape") SdxClusterShape clusterShape, @NotNull(message = "The 'runtimeVersion' query parameter must be specified.") @QueryParam("runtimeVersion") String runtimeVersion, @NotNull(message = "The 'cloudPlatform' query parameter must be specified.") @QueryParam("cloudPlatform") String cloudPlatform, @NotNull(message = "The 'region' query parameter must be specified.") @QueryParam("region") String region, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("crn/{crn}/internal/stoppable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Determines if the datalake can be stopped", operationId ="isStoppable")
    SdxStopValidationResponse isStoppableInternal(@PathParam("crn") String crn, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("/crn/{crn}/vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiates the vertical scaling on Data Lake", operationId ="verticalScalingByCrn")
    FlowIdentifier verticalScalingByCrn(@PathParam("crn") String crn, @Valid StackVerticalScaleV4Request updateRequest);

    @PUT
    @Path("/name/{name}/vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiates the vertical scaling on Data Lake", operationId ="verticalScalingByName")
    FlowIdentifier verticalScalingByName(@PathParam("name") String name, @Valid StackVerticalScaleV4Request updateRequest);

    @PUT
    @Path("/crn/{crn}/submitDatalakeDataSizes/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submits datalake data sizes to Thunderhead DR service", operationId ="submitDatalakeDataSizesInternal")
    void submitDatalakeDataSizesInternal(@PathParam("crn") String crn, @NotNull(message = "The 'operationId' query parameter must be specified.") @QueryParam("operationId") String operationId, @NotNull(message = "The 'dataSizesJson' parameter must be specified.") String dataSizesJson, @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
