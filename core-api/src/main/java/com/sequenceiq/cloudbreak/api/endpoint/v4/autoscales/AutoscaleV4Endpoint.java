package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.springframework.web.bind.annotation.RequestBody;

import com.cloudera.cdp.shaded.javax.ws.rs.core.MediaType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AuthorizeForAutoscaleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.LimitsConfigurationResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.StackOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/autoscale")
@RetryAndMetrics
@Consumes(APPLICATION_JSON)
@Api(value = "/autoscale", description = ControllerDescription.AUTOSCALE_DESCRIPTION, protocols = "http,https",
        consumes = APPLICATION_JSON)
public interface AutoscaleV4Endpoint {

    @PUT
    @Path("/stack/crn/{crn}/{userId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "putStackForAutoscale")
    void putStack(@PathParam("crn") String crn, @PathParam("userId") String userId, @Valid UpdateStackV4Request updateRequest);

    // Not overloading the regular scaling API since 1) that is public, and 2) stopstart may move into a separate InstancePoolManagementController at some point
    @PUT
    @Path("/stack/startNodes/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_START_INSTANCES_BY_ID, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "putStackForAutoscaleStartStop")
    void putStackStartInstances(@PathParam("crn") String crn, @Valid UpdateStackV4Request updateRequest);

    // TODO CB-14929: Remove this API once done with testing, or publish a quick document somewhere on how the put API can be used
    @GET
    @Path("/stack/startNodees/crn/{crn}/{userId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "start_nodes_by_count", produces = APPLICATION_JSON, notes = "blah", nickname = "tmpStartNodes")
    String tmpStartNodes(@PathParam("crn") String crn, @PathParam("userId") String userId,
            @QueryParam("hostGroup") String hostGroup, @QueryParam("numNodes") Integer numNodes);

    @PUT
    @Path("/stack/crn/{crn}/{userId}/cluster")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.PUT_BY_ID, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "putClusterForAutoscale")
    void putCluster(@PathParam("crn") String crn, @PathParam("userId") String userId, @Valid UpdateClusterV4Request updateRequest);

    @GET
    @Path("stack/all")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_ALL, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getAllStackForAutoscale")
    AutoscaleStackV4Responses getAllForAutoscale();

    @GET
    @Path("/autoscale_cluster/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_AUTOSCALE_BY_CRN, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "getAutoscaleClusterByCrn")
    AutoscaleStackV4Response getAutoscaleClusterByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/autoscale_cluster/name/{name}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_AUTOSCALE_BY_NAME, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "getAutoscaleClusterByName")
    AutoscaleStackV4Response getAutoscaleClusterByName(@PathParam("name") String name);

    @GET
    @Path("/autoscale_cluster/name/{name}/internal")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_INTERNAL_AUTOSCALE_BY_NAME, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "getInternalAutoscaleClusterByName")
    AutoscaleStackV4Response getInternalAutoscaleClusterByName(@PathParam("name") String name, @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("/stack/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_CRN, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getStackForAutoscale")
    StackV4Response get(@PathParam("crn") String crn);

    @GET
    @Path("/stack/crn/{crn}/status")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_BY_CRN, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES, nickname = "getStackStatusForAutoscale")
    StackStatusV4Response getStatusByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/stack/crn/{crn}/authorize/{userId}/{tenant}/{permission}")
    @Produces(APPLICATION_JSON)
    AuthorizeForAutoscaleV4Response authorizeForAutoscale(@PathParam("crn") String crn, @PathParam("userId") String userId, @PathParam("tenant") String tenant,
            @PathParam("permission") String permission);

    @GET
    @Path("/stack/crn/{crn}/certificate")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.GET_STACK_CERT, produces = APPLICATION_JSON, notes = Notes.STACK_NOTES,
            nickname = "getCertificateStackForAutoscale")
    CertificateV4Response getCertificate(@PathParam("crn") String crn);

    @DELETE
    @Path("/stack/crn/{crn}/instances")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "decommissionInstancesForClusterCrn")
    void decommissionInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @QueryParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("instanceId") @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("/stack/crn/{crn}/instances/internal")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "decommissionInternalInstancesForClusterCrn")
    void decommissionInternalInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @RequestBody @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("/stack/stopNodes/crn/{crn}/internal")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = StackOpDescription.STOP_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE, produces = APPLICATION_JSON,
            notes = Notes.STACK_NOTES, nickname = "stopInternalInstancesForClusterCrn")
    void stopInternalInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @RequestBody @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("scalingStrategy") ScalingStrategy scalingStrategy);

    // TODO CB-14929: Remove this API once done with testing, or publish a quick document somewhere on how the put API can be used
    @GET
    @Path("/stack/stopNodes2/crn/{crn}/{userId}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "stop_nodes_by_id", produces = APPLICATION_JSON, notes = "blah", nickname = "tmpStopNodes2")
    String tmpStopNodes2(@PathParam("crn") String crn, @PathParam("userId") String userId,
            @QueryParam("hostGroup") String hostGroup, @QueryParam("nodeIds") String nodeIds);

    @GET
    @Path("clusterproxy")
    @Produces(MediaType.APPLICATION_JSON)
    ClusterProxyConfiguration getClusterProxyconfiguration();

    @GET
    @Path("limits_configuration")
    @Produces(MediaType.APPLICATION_JSON)
    LimitsConfigurationResponse getLimitsConfiguration();

    @GET
    @Path("/stack/crn/{crn}/recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    AutoscaleRecommendationV4Response getRecommendation(@PathParam("crn") String clusterCrn);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    AutoscaleRecommendationV4Response getRecommendation(@QueryParam("workspaceId") @Valid Long workspaceId,
                                                        @QueryParam("blueprintName") @NotEmpty String blueprintName);
}
