package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.GET_RECOMMENDATION;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.GET_USED_SUBNETS_BY_ENVIRONMENT_CRN;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.IMD_UPDATE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.LIST_RETRYABLE_FLOWS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.MODIFY_SELINUX_BY_CRN;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.RETRY;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.ROOT_VOLUME_UPDATE_BY_CRN;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.UPDATE_SALT;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.VERTICAL_SCALE_BY_CRN;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.ModifySeLinuxResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageChangeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.FreeIpaRecommendationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.CreateFreeIpaV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.ChangeImageCatalogRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.GenerateImageCatalogResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imdupdate.InstanceMetadataUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DiskUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpdateRootVolumeResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpscaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.start.StartFreeIpaV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.stop.StopFreeIpaV1Response;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/freeipa")
public interface FreeIpaV1Endpoint {

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CREATE, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "createFreeIpaV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CreateFreeIpaV1Response create(@Valid CreateFreeIpaRequest request);

    @POST
    @Path("/attach_child_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.REGISTER_CHILD_ENVIRONMENT, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "attachChildEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void attachChildEnvironment(@Valid AttachChildEnvironmentRequest request);

    @POST
    @Path("/detach_child_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.DEREGISTER_CHILD_ENVIRONMENT, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "detachChildEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void detachChildEnvironment(@Valid DetachChildEnvironmentRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.GET_BY_ENVID, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "getFreeIpaByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeFreeIpaResponse describe(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_GET_BY_ENVID_AND_ACCOUNTID,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalGetFreeIpaByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DescribeFreeIpaResponse describeInternal(@QueryParam("environment") String environmentCrn, @QueryParam("accountId") String accountId);

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.GET_ALL_BY_ENVID, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "getAllFreeIpaByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<DescribeFreeIpaResponse> describeAll(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("internal/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_GET_ALL_BY_ENVID_AND_ACCOUNTID,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalGetAllFreeIpaByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<DescribeFreeIpaResponse> describeAllInternal(
            @QueryParam("environment") String environmentCrn,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.LIST_BY_ACCOUNT, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "listFreeIpaClustersByAccountV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<ListFreeIpaResponse> list();

    @GET
    @Path("internal/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_LIST_BY_ACCOUNT, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "internalListFreeIpaClustersByAccountV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<ListFreeIpaResponse> listInternal(@QueryParam("accountId") String accountId);

    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.HEALTH, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "healthV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    HealthDetailsFreeIpaResponse healthDetails(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("reboot")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.REBOOT, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "rebootV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus rebootInstances(@Valid RebootInstancesRequest request);

    @POST
    @Path("repair")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.REPAIR, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "repairV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus repairInstances(@Valid RepairInstancesRequest request) throws Exception;

    @POST
    @Path("rebuild")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.REBUILD, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "rebuildV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CreateFreeIpaV1Response rebuild(@Valid RebuildRequest request) throws Exception;

    @GET
    @Path("ca.crt")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = FreeIpaOperationDescriptions.GET_ROOTCERTIFICATE_BY_ENVID, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "getFreeIpaRootCertificateByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getRootCertificate(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("internal/ca.crt")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_GET_ROOTCERTIFICATE_BY_ENVID_AND_ACCOUNTID,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalGetFreeIpaRootCertificateByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getRootCertificateInternal(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @QueryParam("accountId") String accountId);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.DELETE_BY_ENVID, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "deleteFreeIpaByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void delete(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @POST
    @Path("cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CLEANUP, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "cleanupV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus cleanup(@Valid CleanupRequest request);

    @POST
    @Path("internal_cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_CLEANUP, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "internalCleanupV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus internalCleanup(@Valid CleanupRequest request, @QueryParam("accountId") String accountId);

    @PUT
    @Path("start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.START, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "startV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StartFreeIpaV1Response start(@QueryParam("environment") @NotEmpty String environmentCrn);

    @PUT
    @Path("stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.STOP, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "stopV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StopFreeIpaV1Response stop(@QueryParam("environment") @NotEmpty String environmentCrn);

    /**
     * @deprecated use rotate_secret endpoint with secret type SALT_PASSWORD instead
     */
    @Deprecated
    @POST
    @Path("rotate_salt_password")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.ROTATE_SALT_PASSWORD,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "rotateSaltPasswordV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSaltPassword(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("cluster-proxy/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.REGISTER_WITH_CLUSTER_PROXY,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "clusterProxyRegisterV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String registerWithClusterProxy(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("cluster-proxy/deregister")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.DEREGISTER_WITH_CLUSTER_PROXY,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "clusterProxyDeregisterV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deregisterWithClusterProxy(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("binduser/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.BIND_USER_CREATE, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "createBindUserV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus createBindUser(@Valid @NotNull BindUserCreateRequest request, @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @POST
    @Path("binduser/create/e2etest")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.BIND_USER_CREATE, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "createE2ETestBindUserV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus createE2ETestBindUser(@Valid @NotNull BindUserCreateRequest request, @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @PUT
    @Path("change_image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CHANGE_IMAGE, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "changeImageV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier changeImage(@Valid @NotNull ImageChangeRequest request);

    @PUT
    @Path("salt_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_SALT, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "updateSaltV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateSaltByName(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("accountId") String accountId);

    @PUT
    @Path("/upscale")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.UPSCALE_FREEIPA, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "upscaleFreeIpaV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpscaleResponse upscale(@Valid UpscaleRequest request);

    @PUT
    @Path("/downscale")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.DOWNSCALE_FREEIPA, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "downscaleFreeIpaV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DownscaleResponse downscale(@Valid DownscaleRequest request);

    @PUT
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RETRY, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "retryV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier retry(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_RETRYABLE_FLOWS, description = FreeIpaNotes.FREEIPA_NOTES, operationId = "listRetryableFlowsV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<RetryableFlowResponse> listRetryableFlows(@QueryParam("environment") @NotEmpty String environmentCrn);

    @PUT
    @Path("change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.CHANGE_IMAGE_CATALOG, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "changeImageCatalog",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void changeImageCatalog(@QueryParam("environment") @NotEmpty String environmentCrn, @Valid @NotNull ChangeImageCatalogRequest changeImageCatalogRequest);

    @GET
    @Path("generate_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.GENERATE_IMAGE_CATALOG, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "generateImageCatalog",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    GenerateImageCatalogResponse generateImageCatalog(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.GET_IMAGE, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "getImageV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    Image getImage(@QueryParam("environment") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @NotEmpty String environmentCrn);

    @PUT
    @Path("/internal/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_UPGRADE_CCM_BY_ENVID,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalUpgradeCcmByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus upgradeCcmInternal(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @GET
    @Path("/internal/default_outbound")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_GET_OUTBOUND_TYPE,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalGetDefaultOutboundByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OutboundType getOutboundType(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @PUT
    @Path("/internal/modify_proxy")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = FreeIpaOperationDescriptions.INTERNAL_MODIFY_PROXY_BY_ENV_ID,
            description = FreeIpaNotes.FREEIPA_NOTES, operationId = "internalModifyProxyConfigByEnvironmentV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    OperationStatus modifyProxyConfigInternal(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @ValidCrn(resource = CrnResourceDescriptor.PROXY) @QueryParam("previousProxy") String previousProxyCrn,
            @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @GET
    @Path("get_recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_RECOMMENDATION,  description = FreeIpaNotes.FREEIPA_NOTES, operationId = "getRecommendationV1",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FreeIpaRecommendationResponse getRecommendation(
            @ValidCrn(resource = CrnResourceDescriptor.CREDENTIAL) @QueryParam("credentialCrn") @NotEmpty String credentialCrn,
            @QueryParam("region") @NotEmpty String region,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("architecture") String architecture);

    @PUT
    @Path("vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = VERTICAL_SCALE_BY_CRN, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "verticalScalingByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    VerticalScaleResponse verticalScalingByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @Valid @NotNull VerticalScaleRequest updateRequest);

    @PUT
    @Path("imd_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = IMD_UPDATE, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "imdUpdate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier instanceMetadataUpdate(@Valid @NotNull InstanceMetadataUpdateRequest request);

    @GET
    @Path("internal/used_subnets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_USED_SUBNETS_BY_ENVIRONMENT_CRN, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "getUsedSubnetsByEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environmentCrn") String environmentCrn);

    @PUT
    @Path("/modify_root_volume")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROOT_VOLUME_UPDATE_BY_CRN, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "updateRootVolumeByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpdateRootVolumeResponse updateRootVolumeByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @Valid @NotNull DiskUpdateRequest rootDiskVolumesRequest);

    @PUT
    @Path("/enforce_selinux")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MODIFY_SELINUX_BY_CRN, description = FreeIpaNotes.FREEIPA_NOTES,
            operationId = "modifySelinuxByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ModifySeLinuxResponse modifySelinuxByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @NotNull @QueryParam("selinux_mode") SeLinux selinuxMode);
}
