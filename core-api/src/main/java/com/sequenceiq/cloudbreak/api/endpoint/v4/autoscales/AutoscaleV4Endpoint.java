package com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Set;

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
import javax.ws.rs.core.MediaType;

import org.springframework.web.bind.annotation.RequestBody;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AuthorizeForAutoscaleV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.AutoscaleStackV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.DependentHostGroupsV4Response;
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
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Path("/autoscale")
@RetryAndMetrics
@Consumes(APPLICATION_JSON)
@Tag(name = "/autoscale", description = ControllerDescription.AUTOSCALE_DESCRIPTION)
public interface AutoscaleV4Endpoint {

    @PUT
    @Path("/stack/crn/{crn}/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.PUT_BY_ID, description =  Notes.STACK_NOTES, operationId ="putStackForAutoscale")
    FlowIdentifier putStack(@PathParam("crn") String crn, @PathParam("userId") String userId, @Valid UpdateStackV4Request updateRequest);

    // Not overloading the regular scaling API since 1) that is public, and 2) stopstart may move into a separate InstancePoolManagementController at some point
    @PUT
    @Path("/stack/startNodes/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.PUT_START_INSTANCES_BY_ID,
            description =  Notes.STACK_NOTES, operationId ="putStackForAutoscaleStartByCrn")
    FlowIdentifier putStackStartInstancesByCrn(@PathParam("crn") String crn, @Valid UpdateStackV4Request updateRequest);

    @PUT
    @Path("/stack/startNodes/name/{name}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.PUT_START_INSTANCES_BY_ID,
            description =  Notes.STACK_NOTES, operationId ="putStackForAutoscaleStartByName")
    FlowIdentifier putStackStartInstancesByName(@PathParam("name") String name, @Valid UpdateStackV4Request updateRequest);

    @PUT
    @Path("/stack/crn/{crn}/{userId}/cluster")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.PUT_BY_ID, description =  Notes.STACK_NOTES, operationId ="putClusterForAutoscale")
    FlowIdentifier putCluster(@PathParam("crn") String crn, @PathParam("userId") String userId, @Valid UpdateClusterV4Request updateRequest);

    @GET
    @Path("stack/all")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_ALL, description =  Notes.STACK_NOTES, operationId ="getAllStackForAutoscale")
    AutoscaleStackV4Responses getAllForAutoscale();

    @GET
    @Path("/autoscale_cluster/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_AUTOSCALE_BY_CRN,
            description =  Notes.STACK_NOTES, operationId ="getAutoscaleClusterByCrn")
    AutoscaleStackV4Response getAutoscaleClusterByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/autoscale_cluster/name/{name}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_AUTOSCALE_BY_NAME,
            description =  Notes.STACK_NOTES, operationId ="getAutoscaleClusterByName")
    AutoscaleStackV4Response getAutoscaleClusterByName(@PathParam("name") String name);

    @GET
    @Path("/autoscale_cluster/name/{name}/internal")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_INTERNAL_AUTOSCALE_BY_NAME,
            description =  Notes.STACK_NOTES, operationId ="getInternalAutoscaleClusterByName")
    AutoscaleStackV4Response getInternalAutoscaleClusterByName(@PathParam("name") String name, @AccountId @QueryParam("accountId") String accountId);

    @GET
    @Path("/stack/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_BY_CRN, description =  Notes.STACK_NOTES, operationId ="getStackForAutoscale")
    StackV4Response get(@PathParam("crn") String crn);

    @GET
    @Path("/stack/crn/{crn}/dependent_host_groups")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_DEPENDENT_HOSTGROUPS_BY_CRN, description =  Notes.STACK_NOTES,
            operationId = "getDependentHostGroupsForMultipleAutoscaleHostGroups")
    DependentHostGroupsV4Response getDependentHostGroupsForMultipleHostGroups(@PathParam("crn") String crn,
            @QueryParam("hostGroups") @NotEmpty Set<String> hostGroups);

    @GET
    @Path("/stack/crn/{crn}/status")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_BY_CRN, description =  Notes.STACK_NOTES, operationId ="getStackStatusForAutoscale")
    StackStatusV4Response getStatusByCrn(@PathParam("crn") String crn);

    @GET
    @Path("/stack/crn/{crn}/authorize/{userId}/{tenant}/{permission}")
    @Produces(APPLICATION_JSON)
    AuthorizeForAutoscaleV4Response authorizeForAutoscale(@PathParam("crn") String crn, @PathParam("userId") String userId, @PathParam("tenant") String tenant,
            @PathParam("permission") String permission);

    @GET
    @Path("/stack/crn/{crn}/certificate")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.GET_STACK_CERT, description =  Notes.STACK_NOTES,
            operationId = "getCertificateStackForAutoscale")
    CertificateV4Response getCertificate(@PathParam("crn") String crn);

    @DELETE
    @Path("/stack/crn/{crn}/instances")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE,
            description =  Notes.STACK_NOTES, operationId ="decommissionInstancesForClusterCrn")
    void decommissionInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @QueryParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("instanceId") @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("/stack/crn/{crn}/instances/internal")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.DELETE_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE,
            description =  Notes.STACK_NOTES, operationId ="decommissionInternalInstancesForClusterCrn")
    FlowIdentifier decommissionInternalInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @RequestBody @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced);

    @DELETE
    @Path("/stack/stopNodes/crn/{crn}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.STOP_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE,
            description =  Notes.STACK_NOTES, operationId ="autoscaleStopInstancesByCrn")
    FlowIdentifier stopInstancesForClusterCrn(@PathParam("crn") String clusterCrn,
            @RequestBody @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("scalingStrategy") ScalingStrategy scalingStrategy);

    @DELETE
    @Path("/stack/stopNodes/name/{name}")
    @Produces(APPLICATION_JSON)
    @Operation(summary =  StackOpDescription.STOP_MULTIPLE_INSTANCES_BY_ID_IN_WORKSPACE,
            description =  Notes.STACK_NOTES, operationId ="autoscaleStopInstancesByName")
    FlowIdentifier stopInstancesForClusterName(@PathParam("name") String clusterName,
            @RequestBody @NotEmpty List<String> instanceIds,
            @QueryParam("forced") @DefaultValue("false") Boolean forced,
            @QueryParam("scalingStrategy") ScalingStrategy scalingStrategy);

    @GET
    @Path("clusterproxy")
    @Produces(MediaType.APPLICATION_JSON)
    ClusterProxyConfiguration getClusterProxyconfiguration();

    @GET
    @Path("limits_configuration")
    @Produces(MediaType.APPLICATION_JSON)
    LimitsConfigurationResponse getLimitsConfiguration(@QueryParam("accountId") String accountId);

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
