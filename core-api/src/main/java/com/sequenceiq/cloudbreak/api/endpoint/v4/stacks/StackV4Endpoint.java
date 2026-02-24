package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.DATAHUB;
import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.PUT_BY_STACK_ID;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.UPDATE_PILLAR_CONFIG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.UPDATE_SALT;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ATTACH_RECIPE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ATTACH_RECIPE_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHANGE_IMAGE_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_FOR_UPGRADE_CLUSTER_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_IMAGE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_IMAGE_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_RDS_UPGRADE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_STACK_UPGRADE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CREATE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CREATE_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DATABASE_BACKUP;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DATABASE_BACKUP_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DATABASE_RESTORE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DATABASE_RESTORE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_BY_NAME_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_INSTANCE_BY_ID_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_WITH_KERBEROS_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DETACH_RECIPE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DETACH_RECIPE_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GENERATE_HOSTS_INVENTORY;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GENERATE_IMAGE_CATALOG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_BY_CRN_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_BY_NAME_WITH_RESOURCES_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_STACK_REQUEST_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_USED_SUBNETS_BY_ENVIRONMENT_CRN;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.IMD_UPDATE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.LIST_BY_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.POST_STACK_FOR_BLUEPRINT_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.PUT_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RANGER_RAZ_ENABLED;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RDS_UPGRADE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RECOVER_CLUSTER_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_ENTITLEMENTS_PARAMS;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_RECIPES_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REFRESH_RECIPES_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REPAIR_CLUSTER_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REPAIR_CLUSTER_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RETRY_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RE_REGISTER_CLUSTER_PROXY_CONFIG;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ROTATE_CERTIFICATES;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.ROTATE_SALT_PASSWORD_BY_CRN_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SALT_PASSWORD_STATUS;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SCALE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STACK_UPGRADE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STACK_UPGRADE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.START_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.START_BY_NAME_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STOP_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STOP_BY_NAME_IN_WORKSPACE_INTERNAL;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SYNC_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SYNC_CM_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPDATE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPDATE_LOAD_BALANCERS;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPDATE_LOAD_BALANCER_IPA_DNS_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPDATE_LOAD_BALANCER_PEM_DNS_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPGRADE_CLUSTER_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPGRADE_OS_IN_WORKSPACE;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.DETERMINE_DATALAKE_DATA_SIZES;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.MODIFY_PROXY_CONFIG_INTERNAL;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ROOT_VOLUME_UPDATE_BY_STACK_CRN;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.ROTATE_STACK_SECRETS;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.SEND_NOTIFICATION;
import static com.sequenceiq.distrox.api.v1.distrox.doc.DistroXOpDescription.VERTICAL_SCALE_BY_NAME;

import java.util.List;
import java.util.Set;

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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackV4SecretRotationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ExternalDatabaseManageDatabaseUserV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackNotificationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerSyncV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.imdupdate.StackInstanceMetadataUpdateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatusResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.BackupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.RestoreV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.imagecatalog.GenerateImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.migraterds.MigrateDatabaseV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v4/{workspaceId}/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v4/{workspaceId}/stacks")
public interface StackV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = LIST_BY_WORKSPACE, description = Notes.STACK_NOTES, operationId = "listStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackViewV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("onlyDatalakes") @DefaultValue("false") boolean onlyDatalakes);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CREATE_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "postStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid StackV4Request request, @QueryParam("accountId") String accountId);

    @POST
    @Path("internal")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CREATE_IN_WORKSPACE_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "postStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response postInternal(@PathParam("workspaceId") Long workspaceId, @Valid StackV4Request request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "getStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @QueryParam("entries") Set<String> entries,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/withResources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_NAME_WITH_RESOURCES_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "getStackInWorkspaceV4WithResources",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response getWithResources(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @QueryParam("entries") Set<String> entries,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("crn/{crn}/withResources")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_NAME_WITH_RESOURCES_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "getStackByCrnInWorkspaceV4WithResources",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response getWithResourcesByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn, @QueryParam("entries") Set<String> entries);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_BY_CRN_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "getStackByCrnInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DELETE_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "deleteStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") boolean forced, @QueryParam("accountId") String accountId);

    @DELETE
    @Path("internal/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DELETE_BY_NAME_IN_WORKSPACE_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "deleteStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void deleteInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") boolean forced,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/update_name_crn_type")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "updateNameCrnAndType",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateNameAndCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("newName") String newName, @QueryParam("newCrn") String newCrn,
            @QueryParam("retainOriginalName") @DefaultValue("false") boolean retainOriginalName);

    @PUT
    @Path("{name}/update_load_balancer_dns")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_LOAD_BALANCER_PEM_DNS_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "updateLoadBalancerPEMDNS",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateLoadBalancerPEMDNS(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/update_load_balancer_dns_ipa")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_LOAD_BALANCER_IPA_DNS_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "updateLoadBalancerIPADNS",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void updateLoadBalancerIPADNS(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("{name}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SYNC_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "syncStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier sync(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @POST
    @Path("internal/{name}/sync_cm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SYNC_CM_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "syncCmInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier syncCm(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @Valid ClouderaManagerSyncV4Request syncRequest);

    @POST
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RETRY_BY_NAME_IN_WORKSPACE, description = Notes.RETRY_STACK_NOTES,
            operationId = "retryStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier retry(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = StackOpDescription.LIST_RETRYABLE_FLOWS, description = Notes.LIST_RETRYABLE_NOTES,
            operationId = "listRetryableFlowsV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<RetryableFlowResponse> listRetryableFlows(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @PUT
    @Path("{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = STOP_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "stopStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStop(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @PUT
    @Path("internal/{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = STOP_BY_NAME_IN_WORKSPACE_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "stopStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStopInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = START_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES, operationId = "startStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStart(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @PUT
    @Path("internal/{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = START_BY_NAME_IN_WORKSPACE_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "startStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putStartInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    /**
     * @deprecated use rotate_secret endpoint with secret type SALT_PASSWORD instead
     */
    @Deprecated
    @POST
    @Path("internal/{crn}/rotate_salt_password")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_SALT_PASSWORD_BY_CRN_IN_WORKSPACE_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "rotateSaltPasswordForStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rotateSaltPasswordInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @Valid RotateSaltPasswordRequest request, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("internal/{crn}/rotate_salt_password/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SALT_PASSWORD_STATUS, description = Notes.STACK_NOTES,
            operationId = "getSaltPasswordStatusForStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SaltPasswordStatusResponse getSaltPasswordStatus(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @PUT
    @Path("internal/{crn}/send_notification")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SEND_NOTIFICATION, description = Notes.STACK_NOTES,
            operationId = "sendNotificationForStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void sendNotification(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn, @Valid StackNotificationV4Request request);

    @PUT
    @Path("internal/{crn}/modify_proxy")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = MODIFY_PROXY_CONFIG_INTERNAL, description = Notes.STACK_NOTES,
            operationId = "modifyProxyConfigInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier modifyProxyConfigInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("previousProxyConfigCrn") String previousProxyConfigCrn, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SCALE_BY_NAME_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "putScalingStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putScaling(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid StackScaleV4Request updateRequest,
            @QueryParam("accountId") String accountId);

    @POST
    @Path("{name}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REPAIR_CLUSTER_IN_WORKSPACE, description = Notes.CLUSTER_REPAIR_NOTES,
            operationId = "repairStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier repairCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid ClusterRepairV4Request clusterRepairRequest, @QueryParam("accountId") String accountId);

    @POST
    @Path("internal/{name}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REPAIR_CLUSTER_IN_WORKSPACE_INTERNAL, description = Notes.CLUSTER_REPAIR_NOTES,
            operationId = "repairStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier repairClusterInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid ClusterRepairV4Request clusterRepairRequest, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPGRADE_CLUSTER_IN_WORKSPACE, description = Notes.CLUSTER_UPGRADE_NOTES,
            operationId = "upgradeOsInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier upgradeOs(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId, @QueryParam("keepVariant") Boolean keepVariant);

    @POST
    @Path("internal/{crn}/os_upgrade_by_upgrade_sets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPGRADE_OS_IN_WORKSPACE, description = "osUpgradeByUpgradeSetsInternalStack")
    FlowIdentifier upgradeOsByUpgradeSetsInternal(@PathParam("workspaceId") Long workspaceId,
            @PathParam("crn") String crn, OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest);

    @POST
    @Path("internal/{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPGRADE_CLUSTER_IN_WORKSPACE, description = Notes.CLUSTER_UPGRADE_NOTES,
            operationId = "upgradeOsInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier upgradeOsInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @QueryParam("keepVariant") Boolean keepVariant);

    @Deprecated
    @GET
    @Path("{name}/check_for_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_FOR_UPGRADE_CLUSTER_IN_WORKSPACE, description = Notes.CHECK_FOR_CLUSTER_UPGRADE_NOTES,
            operationId = "checkForOsUpgradeInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpgradeOptionV4Response checkForOsUpgrade(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @POST
    @Path("{name}/blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = POST_STACK_FOR_BLUEPRINT_IN_WORKSPACE,
            description = Notes.STACK_NOTES, operationId = "postStackForBlueprintV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    GeneratedBlueprintV4Response postStackForBlueprint(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid StackV4Request stackRequest, @QueryParam("accountId") String accountId);

    @PUT
    @Path("{name}/change_image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_IMAGE_IN_WORKSPACE,
            description = Notes.STACK_NOTES, operationId = "changeImageStackInWorkspaceV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier changeImage(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid StackImageChangeV4Request stackImageChangeRequest, @QueryParam("accountId") String accountId);

    @PUT
    @Path("internal/{name}/change_image")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_IMAGE_IN_WORKSPACE_INTERNAL,
            description = Notes.STACK_NOTES, operationId = "changeImageStackInWorkspaceV4Internal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier changeImageInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid StackImageChangeV4Request stackImageChangeRequest, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @DELETE
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DELETE_WITH_KERBEROS_IN_WORKSPACE, description = Notes.CLUSTER_NOTES)
    void deleteWithKerberos(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") boolean forced, @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_STACK_REQUEST_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "getStackRequestFromNameV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackV4Request getRequestfromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_STATUS_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "statusStackV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackStatusV4Response getStatusByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @DELETE
    @Path("{name}/instance")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DELETE_INSTANCE_BY_ID_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "deleteInstanceStackV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteInstance(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") boolean forced, @QueryParam("instanceId") String instanceId,
            @QueryParam("accountId") String accountId);

    @DELETE
    @Path("{name}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, description = Notes.STACK_NOTES,
            operationId = "deleteMultipleInstancesStackV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier deleteMultipleInstances(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("id") @NotEmpty List<String> instances, @QueryParam("forced") @DefaultValue("false") boolean forced,
            @QueryParam("accountId") String accountId);

    @PUT
    @Path("{name}/ambari_password")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = PUT_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "putpasswordStackV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putPassword(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid UserNamePasswordV4Request userNamePasswordJson, @QueryParam("accountId") String accountId);

    @PUT
    @Path("{name}/maintenance")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = SET_MAINTENANCE_MODE_BY_NAME, description = Notes.MAINTENANCE_NOTES,
            operationId = "setClusterMaintenanceMode",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier setClusterMaintenanceMode(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotNull MaintenanceModeV4Request maintenanceMode, @QueryParam("accountId") String accountId);

    @PUT
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = PUT_BY_STACK_ID, description = Notes.CLUSTER_NOTES, operationId = "putClusterV4",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier putCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid UpdateClusterV4Request updateJson,
            @QueryParam("accountId") String accountId);

    @GET
    @Path("{name}/inventory")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = GENERATE_HOSTS_INVENTORY, operationId = "getClusterHostsInventory",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    String getClusterHostsInventory(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId);

    @POST
    @Path("{name}/check_cluster_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_STACK_UPGRADE, operationId = "checkForClusterUpgradeByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpgradeV4Response checkForClusterUpgradeByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotNull UpgradeV4Request request, @QueryParam("accountId") String accountId);

    @POST
    @Path("{name}/cluster_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = STACK_UPGRADE, operationId = "upgradeClusterByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier upgradeClusterByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, String imageId,
            @QueryParam("accountId") String accountId);

    @POST
    @Path("internal/{name}/cluster_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = STACK_UPGRADE_INTERNAL, operationId = "upgradeClusterByNameInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier upgradeClusterByNameInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("imageId") String imageId, @QueryParam("initiatorUserCrn") String initiatorUserCrn,
            @QueryParam("rollingUpgradeEnabled") Boolean rollingUpgradeEnabled);

    @POST
    @Path("internal/{crn}/prepare_cluster_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = STACK_UPGRADE_INTERNAL, operationId = "prepareClusterUpgradeByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier prepareClusterUpgradeByCrnInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("imageId") String imageId, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/{name}/check_rds_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHECK_RDS_UPGRADE_INTERNAL, operationId = "checkUpgradeRdsByNameInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void checkUpgradeRdsByClusterNameInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @PathParam("name") String clusterName,
            @QueryParam("targetVersion") TargetMajorVersion targetMajorVersion,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @QueryParam("initiatorUserCrn")
            String initiatorUserCrn);

    @PUT
    @Path("internal/{name}/rds_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RDS_UPGRADE_INTERNAL, operationId = "upgradeRdsByNameInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RdsUpgradeV4Response upgradeRdsByClusterNameInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @PathParam("name") String clusterName,
            @QueryParam("targetVersion") TargetMajorVersion targetMajorVersion,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) @QueryParam("initiatorUserCrn")
            String initiatorUserCrn, @QueryParam("forced") @DefaultValue("false") boolean forced);

    @Deprecated
    @PUT
    @Path("internal/{name}/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiates the CCM tunnel type upgrade to the latest available version", operationId = "upgradeCcmByNameInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackCcmUpgradeV4Response upgradeCcmByNameInternal(@PathParam("workspaceId") Long workspaceId, @NotEmpty @PathParam("name") String name,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/crn/{crn}/upgrade_ccm")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Initiates the CCM tunnel type upgrade to the latest available version", operationId = "upgradeCcmByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackCcmUpgradeV4Response upgradeCcmByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("internal/{envCrn}/upgrade_ccm_stacks_remaining")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Returns the count of not upgraded stacks for an environment CRN", operationId = "getNotCcmUpgradedStackCountInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    int getNotCcmUpgradedStackCount(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("envCrn") String envCrn,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/{envCrn}/validate_outbound")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validates outbound type for all the stacks in an environment CRN",
            operationId = "validateStackDefaultOutboundInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackOutboundTypeValidationV4Response validateStackOutboundTypes(
            @PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @PathParam("envCrn") String envCrn
    );

    @PUT
    @Path("{name}/salt_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_SALT, operationId = "updateSaltByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateSaltByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("accountId") String accountId, @QueryParam("skipHighstate") @DefaultValue("false") boolean skipHighstate);

    /**
     * @deprecated Use updatePillarConfigurationByCrn instead
     */
    @PUT
    @Path("{name}/pillar_config_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_PILLAR_CONFIG, operationId = "updatePillarConfigurationByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    @Deprecated
    FlowIdentifier updatePillarConfigurationByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("crn/{crn}/pillar_config_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_PILLAR_CONFIG, operationId = "updatePillarConfigurationByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updatePillarConfigurationByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @POST
    @Path("{name}/database_backup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DATABASE_BACKUP, operationId = "databaseBackup",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    @SuppressWarnings("ParameterNumber")
    BackupV4Response backupDatabaseByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("backupId") String backupId,
            @QueryParam("skipDatabaseNames") List<String> skipDatabaseNames,
            @QueryParam("accountId") String accountId,
            @QueryParam("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @QueryParam("dryRun") boolean dryRun);

    @SuppressWarnings("ParameterNumber")
    @POST
    @Path("internal/{name}/database_backup")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DATABASE_BACKUP_INTERNAL, operationId = "databaseBackupInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BackupV4Response backupDatabaseByNameInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("backupId") String backupId, @QueryParam("backupLocation") String backupLocation,
            @QueryParam("closeConnections") boolean closeConnections,
            @QueryParam("skipDatabaseNames") List<String> skipDatabaseNames,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn,
            @QueryParam("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @QueryParam("dryRun") boolean dryRun);

    @SuppressWarnings("ParameterNumber")
    @POST
    @Path("{name}/database_restore")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DATABASE_RESTORE, operationId = "databaseRestore",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RestoreV4Response restoreDatabaseByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("backupId") String backupId,
            @QueryParam("accountId") String accountId,
            @QueryParam("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @QueryParam("dryRun") boolean dryRun);

    @SuppressWarnings("ParameterNumber")
    @POST
    @Path("internal/{name}/database_restore")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DATABASE_RESTORE_INTERNAL, operationId = "databaseRestoreInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RestoreV4Response restoreDatabaseByNameInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("backupLocation") String backupLocation,
            @QueryParam("backupId") String backupId,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn,
            @QueryParam("databaseMaxDurationInMin") int databaseMaxDurationInMin,
            @QueryParam("dryRun") boolean dryRun);

    @POST
    @Path("internal/{name}/cluster_recover")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RECOVER_CLUSTER_IN_WORKSPACE_INTERNAL, operationId = "recoverClusterInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecoveryV4Response recoverClusterByNameInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("internal/{name}/cluster_recoverable")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "validates if the cluster is recoverable or not", operationId = "getClusterRecoverableByNameInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RecoveryValidationV4Response getClusterRecoverableByNameInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("{name}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REFRESH_RECIPES_IN_WORKSPACE, operationId = "refreshStackRecipes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpdateRecipesV4Response refreshRecipes(@PathParam("workspaceId") Long workspaceId, @Valid UpdateRecipesV4Request request,
            @PathParam("name") String name, @QueryParam("accountId") String accountId);

    @PUT
    @Path("internal/{name}/refresh_recipes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REFRESH_RECIPES_IN_WORKSPACE_INTERNAL, operationId = "refreshRecipesInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UpdateRecipesV4Response refreshRecipesInternal(@PathParam("workspaceId") Long workspaceId, @Valid UpdateRecipesV4Request request,
            @PathParam("name") String name, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("{name}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ATTACH_RECIPE_IN_WORKSPACE, operationId = "attachStackRecipe",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AttachRecipeV4Response attachRecipe(@PathParam("workspaceId") Long workspaceId, @Valid AttachRecipeV4Request request,
            @PathParam("name") String name, @QueryParam("accountId") String accountId);

    @POST
    @Path("internal/{name}/attach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ATTACH_RECIPE_IN_WORKSPACE_INTERNAL, operationId = "attachRecipeInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    AttachRecipeV4Response attachRecipeInternal(@PathParam("workspaceId") Long workspaceId, @Valid AttachRecipeV4Request request,
            @PathParam("name") String name, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("{name}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DETACH_RECIPE_IN_WORKSPACE, operationId = "detachStackRecipe",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetachRecipeV4Response detachRecipe(@PathParam("workspaceId") Long workspaceId, @Valid DetachRecipeV4Request request,
            @PathParam("name") String name, @QueryParam("accountId") String accountId);

    @POST
    @Path("internal/{name}/detach_recipe")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DETACH_RECIPE_IN_WORKSPACE_INTERNAL, operationId = "detachRecipeInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    DetachRecipeV4Response detachRecipeInternal(@PathParam("workspaceId") Long workspaceId, @Valid DetachRecipeV4Request request,
            @PathParam("name") String name, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/{name}/rotate_autotls_certificates")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_CERTIFICATES, operationId = "rotateAutoTlsCertificates",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    CertificatesRotationV4Response rotateAutoTlsCertificates(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @Valid CertificatesRotationV4Request rotateCertificateRequest);

    @POST
    @Path("internal/{name}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.StackOpDescription.RENEW_CERTIFICATE,
            description = Notes.RENEW_CERTIFICATE_NOTES, operationId = "renewStackCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier renewCertificate(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/crn/{crn}/renew_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OperationDescriptions.StackOpDescription.RENEW_CERTIFICATE,
            description = Notes.RENEW_CERTIFICATE_NOTES, operationId = "renewInternalStackCertificate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier renewInternalCertificate(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @PUT
    @Path("internal/{name}/update_load_balancers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = UPDATE_LOAD_BALANCERS, operationId = "updateLoadBalancersInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateLoadBalancersInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/{name}/change_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = CHANGE_IMAGE_CATALOG, operationId = "changeImageCatalogInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void changeImageCatalogInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn, @Valid @NotNull ChangeImageCatalogV4Request changeImageCatalogRequest);

    @GET
    @Path("internal/{name}/generate_image_catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GENERATE_IMAGE_CATALOG, operationId = "generateImageCatalogInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    GenerateImageCatalogV4Response generateImageCatalogInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("internal/{crn}/ranger_raz_enabled")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RANGER_RAZ_ENABLED, operationId = "rangerRazEnabled",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RangerRazEnabledV4Response rangerRazEnabledInternal(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/{crn}/re_register_cluster_proxy_config")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = RE_REGISTER_CLUSTER_PROXY_CONFIG, operationId = "reRegisterClusterProxyConfig",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier reRegisterClusterProxyConfig(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("skipFullReRegistration") @DefaultValue("false") boolean skipFullReRegistration, @QueryParam("originalCrn") String originalCrn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/{name}/vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = VERTICAL_SCALE_BY_NAME, description = Notes.STACK_NOTES,
            operationId = "verticalScalingInternalByName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier verticalScalingByName(
            @PathParam("workspaceId") Long workspaceId,
            @PathParam("name") String name,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn,
            @Valid StackVerticalScaleV4Request updateRequest);

    @GET
    @Path("internal/used_subnets")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = GET_USED_SUBNETS_BY_ENVIRONMENT_CRN, description = Notes.STACK_NOTES,
            operationId = "getUsedSubnetsByEnvironment",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(
            @PathParam("workspaceId") Long workspaceId,
            @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) @QueryParam("environmentCrn") String environmentCrn);

    @POST
    @Path("{name}/determine_datalake_data_sizes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = DETERMINE_DATALAKE_DATA_SIZES, operationId = "determineDatalakeDataSizes",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void determineDatalakeDataSizes(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("operationId") String operationId);

    @PUT
    @Path("internal/rotate_secret")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROTATE_STACK_SECRETS, operationId = "rotateStackSecrets",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true), hidden = true)
    FlowIdentifier rotateSecrets(@PathParam("workspaceId") Long workspaceId, @Valid @NotNull StackV4SecretRotationRequest request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/{crn}/refresh_entitlement_params")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = REFRESH_ENTITLEMENTS_PARAMS, operationId = "refreshEntitlementParams",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true), hidden = true)
    FlowIdentifier refreshEntitlementParams(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("{crn}/cm_rolling_restart")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Services rolling restart", operationId = "rollingRestartServices",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier rollingRestartServices(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn,
            @QueryParam("staleServicesOnly") @DefaultValue("false") boolean staleServicesOnly);

    @PUT
    @Path("{name}/trigger_sku_migration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Triggering SKU migration", operationId = "triggerSkuMigration",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier triggerSkuMigration(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("force") @DefaultValue("false") boolean force, @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("imd_update")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = IMD_UPDATE, operationId = "imdUpdate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier instanceMetadataUpdate(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn,
            @NotNull @Valid StackInstanceMetadataUpdateV4Request request);

    @PUT
    @Path("internal/crn/{crn}/rotate_rds_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rotate RDS certificate of the stack by CRN", operationId = "rotateRdsCertificateByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackRotateRdsCertificateV4Response rotateRdsCertificateByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/crn/{crn}/migrate_database_to_ssl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Migrate RDS of the stack by CRN", operationId = "migrateDatabaseByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    MigrateDatabaseV1Response migrateDatabaseByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/crn/{crn}/validate_rotate_rds_certificate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validate that the RDS certificate rotation of the stack triggerable by CRN",
            operationId = "validateRotateRdsCertificateByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void validateRotateRdsCertificateByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/get_database_certificate_status")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Lists all database servers certificate status that are known, either because they were " +
            "registered or because this service created them.",
            operationId = "listDatabaseServersCertificateStatus",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    StackDatabaseServerCertificateStatusV4Responses internalListDatabaseServersCertificateStatus(@PathParam("workspaceId") Long workspaceId,
            @NotNull @Valid StackDatabaseServerCertificateStatusV4Request request,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn
    );

    @POST
    @Path("internal/crn/{crn}/set_default_java_version")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Set default java version on the cluster",
            operationId = "setDefaultJavaVersionByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier setDefaultJavaVersionByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @NotNull @Valid SetDefaultJavaVersionRequest request,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @POST
    @Path("internal/crn/{crn}/set_default_java_version/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Validate default java version is updatable on the cluster to the requested version",
            operationId = "validateDefaultJavaVersionUpdateByCrnInternal",
            responses = @ApiResponse(responseCode = "204", description = "successful operation", useReturnTypeSchema = true))
    void validateDefaultJavaVersionUpdateByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @NotNull @Valid SetDefaultJavaVersionRequest request);

    @GET
    @Path("internal/crn/{crn}/list_available_java_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List available java versions",
            operationId = "listAvailableJavaVersionsByCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<String> listAvailableJavaVersionsByCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn);

    @PUT
    @Path("internal/crn/{crn}/modify_root_volume")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ROOT_VOLUME_UPDATE_BY_STACK_CRN, description = Notes.STACK_NOTES,
            operationId = "updateRootVolumeByStackCrnInternal",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updateRootVolumeByStackCrnInternal(@PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @Valid DiskUpdateRequest rootDiskVolumesRequest,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/manage_db_user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create or delete a database user", operationId = "manageDatabaseUser",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier manageDatabaseUser(@PathParam("workspaceId") Long workspaceId,
            @NotNull @Valid ExternalDatabaseManageDatabaseUserV4Request request,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER})
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @PUT
    @Path("internal/crn/{crn}/modify_selinux/{selinuxMode}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Modifies SELinux on a specific Stack", operationId = "modifySeLinuxByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier modifySeLinuxByCrn(@PathParam("workspaceId") Long workspaceId,
        @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn, @PathParam("selinuxMode") SeLinux selinuxMode);

    @PUT
    @Path("internal/crn/{crn}/update_dns_entries")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the public DNS entries of the cluster",
            operationId = "updatePublicDnsEntriesByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    FlowIdentifier updatePublicDnsEntriesByCrn(
            @PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);

    @GET
    @Path("internal/get_clusters_names_by_encryption_profile/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get clusters name using given encryption profile",
            operationId = "getClustersNamesByEncryptionProfile",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    List<String> getClustersNamesByEncryptionProfile(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String encryptionProfileCrn);

    @PUT
    @Path("internal/crn/{crn}/modify_notification_state")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update the notification state of the cluster by CRN",
            operationId = "modifyNotificationStateByCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    void modifyNotificationStateByCrn(
            @PathParam("workspaceId") Long workspaceId,
            @NotEmpty @ValidCrn(resource = {DATAHUB, VM_DATALAKE}) @PathParam("crn") String crn,
            @QueryParam("initiatorUserCrn") String initiatorUserCrn);
}
