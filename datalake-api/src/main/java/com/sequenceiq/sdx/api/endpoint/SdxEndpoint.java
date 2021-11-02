package com.sequenceiq.sdx.api.endpoint;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHANGE_IMAGE_CATALOG;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.validation.ValidStackNameFormat;
import com.sequenceiq.cloudbreak.validation.ValidStackNameLength;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import com.sequenceiq.sdx.api.model.SdxChangeImageCatalogRequest;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxCustomClusterRequest;
import com.sequenceiq.sdx.api.model.SdxRepairRequest;
import com.sequenceiq.sdx.api.model.SdxSyncComponentVersionsFromCmResponse;
import com.sequenceiq.sdx.api.model.SdxValidateCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SetRangerCloudIdentityMappingRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Validated
@Path("/sdx")
@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/sdx", protocols = "http,https", consumes = MediaType.APPLICATION_JSON)
public interface SdxEndpoint {

    @POST
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "create SDX cluster", produces = "application/json", nickname = "createSdx")
    SdxClusterResponse create(@ValidStackNameFormat @ValidStackNameLength @PathParam("name") String name,
            @Valid SdxClusterRequest createSdxClusterRequest);

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list SDX clusters", produces = MediaType.APPLICATION_JSON, nickname = "listSdx")
    List<SdxClusterResponse> list(@QueryParam("envName") String envName, @DefaultValue("false") @QueryParam("includeDetached") boolean includeDetached);

    @GET
    @Path("list/internal")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list SDX clusters internally", produces = MediaType.APPLICATION_JSON, nickname = "listSdxInternal")
    List<SdxClusterResponse> internalList(@AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster", produces = MediaType.APPLICATION_JSON, nickname = "getSdx")
    SdxClusterResponse get(@PathParam("name") String name);

    @GET
    @Path("/crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster by crn", produces = MediaType.APPLICATION_JSON, nickname = "getSdxByCrn")
    SdxClusterResponse getByCrn(@PathParam("clusterCrn") @ValidCrn(resource = CrnResourceDescriptor.DATALAKE) String clusterCrn);

    @GET
    @Path("/envcrn/{envCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster by environment crn", produces = MediaType.APPLICATION_JSON, nickname = "getSdxByEnvCrn")
    List<SdxClusterResponse> getByEnvCrn(@PathParam("envCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn);

    @GET
    @Path("{name}/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster detail", produces = MediaType.APPLICATION_JSON, nickname = "getSdxDetail")
    SdxClusterDetailResponse getDetail(@PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("/crn/{clusterCrn}/detail")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "get SDX cluster detail by crn", produces = MediaType.APPLICATION_JSON, nickname = "getSdxDetailByCrn")
    SdxClusterDetailResponse getDetailByCrn(@PathParam("clusterCrn") String clusterCrn, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "delete SDX cluster", produces = "application/json", nickname = "deleteSdx")
    FlowIdentifier delete(@PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("{name}/resize")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Re-size SDX cluster", produces = "application/json", nickname = "resizeSdx")
    SdxClusterResponse resize(@PathParam("name") String name, @Valid SdxClusterResizeRequest resizeSdxClusterRequest);

    @DELETE
    @Path("/crn/{clusterCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "delete SDX cluster by crn", produces = "application/json", nickname = "deleteSdxByCrn")
    FlowIdentifier deleteByCrn(@PathParam("clusterCrn") String clusterCrn, @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @POST
    @Path("{name}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "repairs an sdxNode in the specified hostgroup", nickname = "repairSdxNode")
    FlowIdentifier repairCluster(@PathParam("name") String name, SdxRepairRequest clusterRepairRequest);

    @POST
    @Path("/crn/{crn}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "repairs an sdxNode in the specified hostgroup by crn", nickname = "repairSdxNodeByCrn")
    FlowIdentifier repairClusterByCrn(@PathParam("crn") String crn, SdxRepairRequest clusterRepairRequest);

    @POST
    @Path("/crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "renew certificate on SDX cluster by crn", nickname = "renewCertificateOnSdxByCrn")
    @ApiResponses({
            @ApiResponse(code = 200, message = "successful operation", response = FlowIdentifier.class),
            @ApiResponse(code = 0, message = "unsuccessful operation", response = Void.class)
    })
    FlowIdentifier renewCertificate(@PathParam("crn") String crn);

    @POST
    @Path("{name}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "sync SDX cluster by name", produces = MediaType.APPLICATION_JSON, nickname = "syncSdx")
    void sync(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "sync SDX cluster by crn", produces = MediaType.APPLICATION_JSON, nickname = "syncSdxByCrn")
    void syncByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "retry sdx", produces = MediaType.APPLICATION_JSON, nickname = "retrySdx")
    FlowIdentifier retry(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "retry sdx by crn", produces = MediaType.APPLICATION_JSON, nickname = "retrySdxByCrn")
    FlowIdentifier retryByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "start sdx", produces = MediaType.APPLICATION_JSON, nickname = "startSdxByName")
    FlowIdentifier startByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "start sdx by crn", produces = MediaType.APPLICATION_JSON, nickname = "startSdxByCrn")
    FlowIdentifier startByCrn(@PathParam("crn") String crn);

    @POST
    @Path("{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "stop sdx", produces = MediaType.APPLICATION_JSON, nickname = "stopSdxByName")
    FlowIdentifier stopByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "stop sdx by crn", produces = MediaType.APPLICATION_JSON, nickname = "stopSdxByCrn")
    FlowIdentifier stopByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/versions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list datalake versions", produces = MediaType.APPLICATION_JSON, nickname = "versions")
    List<String> versions(@QueryParam("cloudPlatform") String cloudPlatform);

    @GET
    @Path("/advertisedruntimes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "list advertised datalake versions", produces = MediaType.APPLICATION_JSON, nickname = "advertisedruntimes")
    List<AdvertisedRuntime> advertisedRuntimes(@QueryParam("cloudPlatform") String cloudPlatform);

    @POST
    @Path("/envcrn/{envCrn}/ranger_cloud_identity_mapping")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Set ranger cloud identity mapping", produces = MediaType.APPLICATION_JSON, nickname = "setRangerCloudIdentityMapping")
    RangerCloudIdentitySyncStatus setRangerCloudIdentityMapping(@PathParam("envCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn,
            @NotNull @Valid SetRangerCloudIdentityMappingRequest request);

    @GET
    @Path("/envcrn/{envCrn}/ranger_cloud_identity_sync_status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get status of a ranger cloud identity sync", produces = MediaType.APPLICATION_JSON, nickname = "getRangerCloudIdentitySyncStatus")
    RangerCloudIdentitySyncStatus getRangerCloudIdentitySyncStatus(@PathParam("envCrn") @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn,
            long commandId);

    @PUT
    @Path("{name}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ROTATE_CERTIFICATES, nickname = "rotateAutoTlsCertificatesByName")
    FlowIdentifier rotateAutoTlsCertificatesByName(@PathParam("name") String name, @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("crn/{crn}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ROTATE_CERTIFICATES, nickname = "rotateAutoTlsCertificatesByCrn")
    FlowIdentifier rotateAutoTlsCertificatesByCrn(@PathParam("crn") String crn, @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @PUT
    @Path("/crn/{crn}/enable_ranger_raz")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Set the rangerRazEnabled flag of the cluster if Raz is installed manually", nickname = "enableRangerRazByCrn")
    void enableRangerRazByCrn(@PathParam("crn") String crn);

    @PUT
    @Path("/name/{name}/enable_ranger_raz")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Set the rangerRazEnabled flag of the cluster if Raz is installed manually", nickname = "enableRangerRazByName")
    void enableRangerRazByName(@PathParam("name") String name);

    @POST
    @Path("{name}/custom_image")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "create custom SDX cluster", produces = MediaType.APPLICATION_JSON, nickname = "createCustomSdx")
    SdxClusterResponse create(@PathParam("name") String name, @Valid SdxCustomClusterRequest createSdxClusterRequest);

    @POST
    @Path("validate_cloud_storage/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "validate cloud storage", produces = MediaType.APPLICATION_JSON, nickname = "validateCloudStorage")
    ObjectStorageValidateResponse validateCloudStorage(@ValidStackNameFormat @ValidStackNameLength @PathParam("name") String clusterName,
            @Valid SdxValidateCloudStorageRequest sdxValidateCloudStorageRequest);

    @PUT
    @Path("{name}/change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CHANGE_IMAGE_CATALOG, nickname = "changeImageCatalog")
    void changeImageCatalog(@PathParam("name") String name, @Valid @NotNull SdxChangeImageCatalogRequest changeImageCatalogRequest);

    @POST
    @Path("{name}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "syncs CM and parcel versions from CM and updates SDX cluster version", nickname = "syncCmOnDatalakeCluster")
    SdxSyncComponentVersionsFromCmResponse syncComponentVersionsFromCmByName(@PathParam("name") String name);

    @POST
    @Path("/crn/{crn}/sync_component_versions_from_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "syncs CM and parcel versions from CM and updates SDX cluster version", nickname = "syncCmOnDatalakeClusterByCrn")
    SdxSyncComponentVersionsFromCmResponse syncComponentVersionsFromCmByCrn(@PathParam("crn") String crn);

    @GET
    @Path("instance_group_names")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Gather available instance group names by SDX cluster attributes", nickname = "getInstanceGroupNamesBySdxDetails")
    Set<String> getInstanceGroupNamesBySdxDetails(@QueryParam("clusterShape") SdxClusterShape clusterShape,
            @QueryParam("runtimeVersion") String runtimeVersion, @QueryParam("cloudPlatform") String cloudPlatform);

}
