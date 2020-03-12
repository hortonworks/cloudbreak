package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.PUT_BY_STACK_ID;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_FOR_UPGRADE_CLUSTER_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_IMAGE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_STACK_UPGRADE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CREATE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_INSTANCE_BY_ID_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_WITH_KERBEROS_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GENERATE_HOSTS_INVENTORY;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_STACK_REQUEST_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.LIST_BY_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.POST_STACK_FOR_BLUEPRINT_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.PUT_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REPAIR_CLUSTER_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RETRY_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SCALE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STACK_UPGRADE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.START_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STOP_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SYNC_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.UPGRADE_CLUSTER_IN_WORKSPACE;

import java.util.List;
import java.util.Set;

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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v4/{workspaceId}/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/stacks", protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface StackV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_BY_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "listStackInWorkspaceV4")
    StackViewV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("onlyDatalakes") @DefaultValue("false") boolean onlyDatalakes);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CREATE_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "postStackInWorkspaceV4")
    StackV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid StackV4Request request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getStackInWorkspaceV4")
    StackV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "deleteStackInWorkspaceV4")
    void delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @POST
    @Path("{name}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SYNC_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "syncStackInWorkspaceV4")
    FlowIdentifier sync(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RETRY_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.RETRY_STACK_NOTES,
            nickname = "retryStackInWorkspaceV4")
    FlowIdentifier retry(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.LIST_RETRYABLE_FLOWS, produces = MediaType.APPLICATION_JSON, notes = Notes.LIST_RETRYABLE_NOTES,
            nickname = "listRetryableFlowsV4")
    List<RetryableFlowResponse> listRetryableFlows(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = STOP_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "stopStackInWorkspaceV4")
    FlowIdentifier putStop(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = START_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "startStackInWorkspaceV4")
    FlowIdentifier putStart(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SCALE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "putScalingStackInWorkspaceV4")
    FlowIdentifier putScaling(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid StackScaleV4Request updateRequest);

    @POST
    @Path("{name}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = REPAIR_CLUSTER_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_REPAIR_NOTES,
            nickname = "repairStackInWorkspaceV4")
    FlowIdentifier repairCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid ClusterRepairV4Request clusterRepairRequest);

    @POST
    @Path("{name}/upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = UPGRADE_CLUSTER_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_UPGRADE_NOTES,
            nickname = "upgradeClusterInWorkspaceV4")
    FlowIdentifier upgradeCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/check_for_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CHECK_FOR_UPGRADE_CLUSTER_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.CHECK_FOR_CLUSTER_UPGRADE_NOTES,
            nickname = "checkForUpgradeInWorkspaceV4")
    UpgradeOptionV4Response checkForUpgrade(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("{name}/blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = POST_STACK_FOR_BLUEPRINT_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "postStackForBlueprintV4")
    GeneratedBlueprintV4Response postStackForBlueprint(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid StackV4Request stackRequest);

    @PUT
    @Path("{name}/change_image")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CHECK_IMAGE_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "changeImageStackInWorkspaceV4")
    FlowIdentifier changeImage(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid StackImageChangeV4Request stackImageChangeRequest);

    @DELETE
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_WITH_KERBEROS_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_NOTES)
    void deleteWithKerberos(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_STACK_REQUEST_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getStackRequestFromNameV4")
    StackV4Request getRequestfromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_STATUS_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "statusStackV4")
    StackStatusV4Response getStatusByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}/instance")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_INSTANCE_BY_ID_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstanceStackV4")
    FlowIdentifier deleteInstance(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") boolean forced, @QueryParam("instanceId") String instanceId);

    @DELETE
    @Path("{name}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteMultipleInstancesStackV4")
    FlowIdentifier deleteMultipleInstances(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("id") @NotEmpty List<String> instances,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    @PUT
    @Path("{name}/ambari_password")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PUT_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "putpasswordStackV4")
    FlowIdentifier putPassword(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid UserNamePasswordV4Request userNamePasswordJson);

    @PUT
    @Path("{name}/maintenance")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SET_MAINTENANCE_MODE_BY_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.MAINTENANCE_NOTES,
            nickname = "setClusterMaintenanceMode")
    FlowIdentifier setClusterMaintenanceMode(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotNull MaintenanceModeV4Request maintenanceMode);

    @PUT
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PUT_BY_STACK_ID, produces = MediaType.APPLICATION_JSON, notes = Notes.CLUSTER_NOTES, nickname = "putClusterV4")
    FlowIdentifier putCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid UpdateClusterV4Request updateJson);

    @GET
    @Path("{name}/inventory")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = GENERATE_HOSTS_INVENTORY, produces = MediaType.TEXT_PLAIN, nickname = "getClusterHostsInventory")
    String getClusterHostsInventory(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/check_stack_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CHECK_STACK_UPGRADE, nickname = "checkForUpgradeByName")
    UpgradeOptionsV4Response checkForStackUpgradeByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("{name}/stack_upgrade")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = STACK_UPGRADE, nickname = "upgradeStackByName")
    FlowIdentifier upgradeStackByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, String imageId);

}
