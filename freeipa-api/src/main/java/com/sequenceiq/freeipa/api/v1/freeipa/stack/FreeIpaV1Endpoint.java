package com.sequenceiq.freeipa.api.v1.freeipa.stack;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.LIST_RETRYABLE_FLOWS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.RETRY;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions.UPDATE_SALT;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.freeipa.api.FreeIpaApi;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaNotes;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaOperationDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageChangeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.ChangeImageCatalogRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.imagecatalog.GenerateImageCatalogResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@RetryAndMetrics
@Path("/v1/freeipa")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/freeipa",
        protocols = "http,https",
        authorizations = {@Authorization(value = FreeIpaApi.CRN_HEADER_API_KEY)},
        consumes = MediaType.APPLICATION_JSON)
public interface FreeIpaV1Endpoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CREATE, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "createFreeIpaV1")
    DescribeFreeIpaResponse create(@Valid CreateFreeIpaRequest request);

    @POST
    @Path("/attach_child_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REGISTER_CHILD_ENVIRONMENT, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "attachChildEnvironmentV1")
    void attachChildEnvironment(@Valid AttachChildEnvironmentRequest request);

    @POST
    @Path("/detach_child_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.DEREGISTER_CHILD_ENVIRONMENT, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "detachChildEnvironmentV1")
    void detachChildEnvironment(@Valid DetachChildEnvironmentRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.GET_BY_ENVID, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "getFreeIpaByEnvironmentV1")
    DescribeFreeIpaResponse describe(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.INTERNAL_GET_BY_ENVID_AND_ACCOUNTID, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "internalGetFreeIpaByEnvironmentV1")
    DescribeFreeIpaResponse describeInternal(@QueryParam("environment") String environmentCrn, @QueryParam("accountId") @AccountId String accountId);

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.GET_ALL_BY_ENVID, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "getAllFreeIpaByEnvironmentV1")
    List<DescribeFreeIpaResponse> describeAll(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("internal/all")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.INTERNAL_GET_ALL_BY_ENVID_AND_ACCOUNTID, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "internalGetAllFreeIpaByEnvironmentV1")
    List<DescribeFreeIpaResponse> describeAllInternal(
            @QueryParam("environment") String environmentCrn,
            @QueryParam("accountId") @AccountId String accountId);

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.LIST_BY_ACCOUNT, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "listFreeIpaClustersByAccountV1")
    List<ListFreeIpaResponse> list();

    @GET
    @Path("internal/list")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.INTERNAL_LIST_BY_ACCOUNT, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "internalListFreeIpaClustersByAccountV1")
    List<ListFreeIpaResponse> listInternal(@QueryParam("accountId") @AccountId String accountId);

    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.HEALTH, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "healthV1")
    HealthDetailsFreeIpaResponse healthDetails(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("reboot")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REBOOT, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "rebootV1")
    OperationStatus rebootInstances(@Valid RebootInstancesRequest request);

    @POST
    @Path("repair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REPAIR, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "repairV1")
    OperationStatus repairInstances(@Valid RepairInstancesRequest request) throws Exception;

    @POST
    @Path("rebuild")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REBUILD, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "rebuildV1")
    DescribeFreeIpaResponse rebuild(@Valid RebuildRequest request) throws Exception;

    @GET
    @Path("ca.crt")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = FreeIpaOperationDescriptions.GET_ROOTCERTIFICATE_BY_ENVID, produces = MediaType.TEXT_PLAIN, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "getFreeIpaRootCertificateByEnvironmentV1")
    String getRootCertificate(@QueryParam("environment") @NotEmpty String environmentCrn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.DELETE_BY_ENVID, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "deleteFreeIpaByEnvironmentV1")
    void delete(@QueryParam("environment") @NotEmpty String environmentCrn, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @POST
    @Path("cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CLEANUP, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "cleanupV1")
    OperationStatus cleanup(@Valid CleanupRequest request);

    @POST
    @Path("internal_cleanup")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.INTERNAL_CLEANUP, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "internalCleanupV1")
    OperationStatus internalCleanup(@Valid CleanupRequest request, @QueryParam("accountId") @AccountId String accountId);

    @PUT
    @Path("start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.START, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "startV1")
    void start(@QueryParam("environment") @NotEmpty String environmentCrn);

    @PUT
    @Path("stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.STOP, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "stopV1")
    void stop(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("cluster-proxy/register")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.REGISTER_WITH_CLUSTER_PROXY, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "clusterProxyRegisterV1")
    String registerWithClusterProxy(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("cluster-proxy/deregister")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.DEREGISTER_WITH_CLUSTER_PROXY, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "clusterProxyDeregisterV1")
    void deregisterWithClusterProxy(@QueryParam("environment") @NotEmpty String environmentCrn);

    @POST
    @Path("binduser/create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.BIND_USER_CREATE, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "createBindUserV1")
    OperationStatus createBindUser(@Valid @NotNull BindUserCreateRequest request, @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @POST
    @Path("binduser/create/e2etest")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.BIND_USER_CREATE, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "createE2ETestBindUserV1")
    OperationStatus createE2ETestBindUser(@Valid @NotNull BindUserCreateRequest request, @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);

    @PUT
    @Path("change_image")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CHANGE_IMAGE, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "changeImageV1")
    FlowIdentifier changeImage(@Valid @NotNull ImageChangeRequest request);

    @PUT
    @Path("salt_update")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UPDATE_SALT, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "updateSaltV1")
    FlowIdentifier updateSaltByName(@QueryParam("environment") @NotEmpty String environmentCrn, @AccountId @QueryParam("accountId") String accountId);

    @PUT
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RETRY, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "retryV1")
    FlowIdentifier retry(@QueryParam("environment") @NotEmpty String environmentCrn);

    @GET
    @Path("retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_RETRYABLE_FLOWS,  produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "listRetryableFlowsV1")
    List<RetryableFlowResponse> listRetryableFlows(@QueryParam("environment") @NotEmpty String environmentCrn);

    @PUT
    @Path("change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.CHANGE_IMAGE_CATALOG, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "changeImageCatalog")
    void changeImageCatalog(@QueryParam("environment") @NotEmpty String environmentCrn, @Valid @NotNull ChangeImageCatalogRequest changeImageCatalogRequest);

    @GET
    @Path("generate_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.GENERATE_IMAGE_CATALOG, produces = MediaType.APPLICATION_JSON, notes = FreeIpaNotes.FREEIPA_NOTES,
            nickname = "generateImageCatalog")
    GenerateImageCatalogResponse generateImageCatalog(@QueryParam("environment") @NotEmpty String environmentCrn);

    @PUT
    @Path("/internal/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = FreeIpaOperationDescriptions.INTERNAL_UPGRADE_CCM_BY_ENVID, produces = MediaType.APPLICATION_JSON,
            notes = FreeIpaNotes.FREEIPA_NOTES, nickname = "internalUpgradeCcmByEnvironmentV1")
    OperationStatus upgradeCcmInternal(@ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environment") @NotEmpty String environmentCrn,
            @ValidCrn(resource = CrnResourceDescriptor.USER) @QueryParam("initiatorUserCrn") @NotEmpty String initiatorUserCrn);
}
