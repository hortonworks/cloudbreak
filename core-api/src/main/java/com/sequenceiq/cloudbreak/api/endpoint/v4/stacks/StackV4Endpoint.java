package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks;

import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.PUT_BY_STACK_ID;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterOpDescription.SET_MAINTENANCE_MODE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CHECK_IMAGE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.CREATE_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_INSTANCE_BY_ID_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.DELETE_WITH_KERBEROS_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_STACK_REQUEST_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.GET_STATUS_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.LIST_BY_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.POST_STACK_FOR_CLUSTER_DEFINITION_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.PUT_BY_NAME;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.REPAIR_CLUSTER_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.RETRY_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SCALE_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.START_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.STOP_BY_NAME_IN_WORKSPACE;
import static com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription.SYNC_BY_NAME_IN_WORKSPACE;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ReinstallV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.Notes;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/stacks")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/stacks", protocols = "http,https")
public interface StackV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "listStackInWorkspaceV4")
    StackViewV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("onlyDatalakes") @DefaultValue("false") Boolean onlyDatalakes);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "postStackInWorkspaceV4")
    StackV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid StackV4Request request);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "getStackInWorkspaceV4")
    StackV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @QueryParam("entries") Set<String> entries);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "deleteStackInWorkspaceV4")
    void delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    @PUT
    @Path("{name}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SYNC_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "syncStackInWorkspaceV4")
    void putSync(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = RETRY_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.RETRY_STACK_NOTES, nickname = "retryStackInWorkspaceV4")
    void putRetry(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = STOP_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "stopStackInWorkspaceV4")
    void putStop(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = START_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "startStackInWorkspaceV4")
    void putStart(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SCALE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES, nickname = "putScalingStackInWorkspaceV4")
    void putScaling(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid StackScaleV4Request updateRequest);

    @POST
    @Path("{name}/manual_repair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = REPAIR_CLUSTER_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CLUSTER_REPAIR_NOTES, nickname = "repairStackInWorkspaceV4")
    void repairCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid ClusterRepairV4Request clusterRepairRequest);

    @POST
    @Path("{name}/cluster_definition")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = POST_STACK_FOR_CLUSTER_DEFINITION_IN_WORKSPACE, produces = ContentType.JSON,
            notes = Notes.STACK_NOTES, nickname = "postStackForClusterDefinitionV4")
    GeneratedClusterDefinitionV4Response postStackForClusterDefinition(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid StackV4Request stackRequest);

    @PUT
    @Path("{name}/change_image")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = CHECK_IMAGE_IN_WORKSPACE, produces = ContentType.JSON,
            notes = Notes.STACK_NOTES, nickname = "changeImageStackInWorkspaceV4")
    void changeImage(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid StackImageChangeV4Request stackImageChangeRequest);

    @DELETE
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_WITH_KERBEROS_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES)
    void deleteWithKerberos(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("withStackDelete") @DefaultValue("false") Boolean withStackDelete,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_STACK_REQUEST_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "getStackRequestFromNameV4")
    StackV4Request getRequestfromName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = GET_STATUS_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "statusStackV4")
    StackStatusV4Response getStatusByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}/instance")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = DELETE_INSTANCE_BY_ID_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstanceStackV4")
    void deleteInstance(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") Boolean forced, @QueryParam("instanceId") String instanceId);

    @PUT
    @Path("{name}/reinstall")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PUT_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putReinstallStackV4")
    void putReinstall(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid ReinstallV4Request reinstallRequestV2);

    @PUT
    @Path("{name}/ambari_password")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PUT_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putpasswordStackV4")
    void putPassword(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid UserNamePasswordV4Request userNamePasswordJson);

    @PUT
    @Path("{name}/maintenance")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = SET_MAINTENANCE_MODE, produces = ContentType.JSON, notes = Notes.MAINTENANCE_NOTES,
            nickname = "setClusterMaintenanceMode")
    void setClusterMaintenanceMode(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotNull MaintenanceModeV4Request maintenanceMode);

    @PUT
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = PUT_BY_STACK_ID, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES, nickname = "putClusterV4")
    void putCluster(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name, @Valid UpdateClusterV4Request updateJson);
}
