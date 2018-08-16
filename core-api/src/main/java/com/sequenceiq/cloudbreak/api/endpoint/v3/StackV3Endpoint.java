package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
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
import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.api.model.GeneratedBlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ReinstallRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.StackImageChangeRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/stack")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/stack", description = ControllerDescription.STACK_V3_DESCRIPTION, protocols = "http,https")
public interface StackV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "listStacksByOrganization")
    Set<StackResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getStackInOrganization")
    StackResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "createStackInOrganization")
    StackResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid StackV2Request request);

    // deleteStackV2
    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deleteStackInOrganization")
    void deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    // putsyncStackV2
    @PUT
    @Path("{name}/sync")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.SYNC_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putsyncStackV3")
    Response putSyncInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    // retryStack
    @POST
    @Path("{name}/retry")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.RETRY_BY_NAME_IN_ORG,
            produces = ContentType.JSON, notes = Notes.RETRY_STACK_NOTES, nickname = "retryStackV3")
    void retryInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    // putstopStackV2
    @PUT
    @Path("{name}/stop")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.STOP_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putstopStackV3")
    Response putStopInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    // putstartStackV2
    @PUT
    @Path("{name}/start")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.START_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putstartStackV3")
    Response putStartInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    // putscalingStackV2
    @PUT
    @Path("{name}/scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.SCALE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putscalingStackV3")
    Response putScalingInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @Valid StackScaleRequestV2 updateRequest);

    // repairCluster // v1
    @POST
    @Path("{name}/manualrepair")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.REPAIR_CLUSTER_IN_ORG, produces = ContentType.JSON, notes = Notes.CLUSTER_REPAIR_NOTES,
            nickname = "repairClusterV3")
    Response repairClusterInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            ClusterRepairRequest clusterRepairRequest);

    // deleteCluster // v1
    @DELETE
    @Path("{name}/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_WITH_KERBEROS_IN_ORG, produces = ContentType.JSON, notes = Notes.CLUSTER_NOTES,
            nickname = "deleteClusterWithKerberosV3")
    void deleteWithKerberosInOrg(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @QueryParam("withStackDelete") @DefaultValue("false") Boolean withStackDelete,
            @QueryParam("deleteDependencies") @DefaultValue("false") Boolean deleteDependencies);

    // getClusterRequestFromName
    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_REQUEST_IN_ORG, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "getStackRequestFromNameV3")
    StackV2Request getRequestfromName(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    // postPublicStackV2ForBlueprint
    @POST
    @Path("{name}/blueprint")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.POST_STACK_FOR_BLUEPRINT_IN_ORG, produces = ContentType.JSON,
            notes = Notes.STACK_NOTES, nickname = "postStackForBlueprintV3")
    GeneratedBlueprintResponse postStackForBlueprint(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @Valid StackV2Request stackRequest) throws Exception;

    // deleteInstanceStackV2
    @DELETE
    @Path("{name}/instance/{instanceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_INSTANCE_BY_ID_IN_ORG, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "deleteInstanceStackV3")
    Response deleteInstance(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @PathParam("instanceId") String instanceId,
            @QueryParam("forced") @DefaultValue("false") boolean forced);

    // changeImage
    @PUT
    @Path("{name}/changeImage")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.CHECK_IMAGE_IN_ORG, produces = ContentType.JSON,
            notes = Notes.STACK_NOTES, nickname = "changeImageV3")
    Response changeImage(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @Valid StackImageChangeRequest stackImageChangeRequest);

    // PutreinstallStackV2
    @PUT
    @Path("{name}/reinstall}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_NAME, produces = ContentType.JSON, notes = Notes.STACK_NOTES,
            nickname = "putreinstallStackV3")
    Response putReinstall(@PathParam("organizationId") Long organizationId, @PathParam("name") String name,
            @Valid ReinstallRequestV2 reinstallRequestV2);
}
