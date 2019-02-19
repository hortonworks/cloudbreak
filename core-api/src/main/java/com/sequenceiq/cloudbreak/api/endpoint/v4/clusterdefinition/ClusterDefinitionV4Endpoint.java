package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.CLUSTER_DEFINITION_NOTES;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/clusterdefinitions")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/clusterdefinitions", description = ControllerDescription.CLUSTER_DEFINITION_V4_DESCRIPTION, protocols = "http,https")
public interface ClusterDefinitionV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterDefinitionOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = CLUSTER_DEFINITION_NOTES,
            nickname = "listClusterDefinitionsByWorkspace")
    ClusterDefinitionV4ViewResponses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterDefinitionOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = JSON, notes = CLUSTER_DEFINITION_NOTES,
            nickname = "getClusterDefinitionInWorkspace")
    ClusterDefinitionV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterDefinitionOpDescription.CREATE_IN_WORKSPACE, produces = JSON, notes = CLUSTER_DEFINITION_NOTES,
            nickname = "createClusterDefinitionInWorkspace")
    ClusterDefinitionV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid ClusterDefinitionV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterDefinitionOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = CLUSTER_DEFINITION_NOTES,
            nickname = "deleteClusterDefinitionInWorkspace")
    ClusterDefinitionV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterDefinitionOpDescription.GET_BY_NAME, produces = JSON, notes = CLUSTER_DEFINITION_NOTES,
            nickname = "getClusterDefinitionRequestFromName")
    ClusterDefinitionV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("{name}/parameters")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.UtilityOpDescription.CUSTOM_PARAMETERS, produces = ContentType.JSON,
            nickname = "getClusterDefinitionCustomParameters")
    ParametersQueryV4Response getParameters(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_RECOMMENDATION, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendationForWorkspace")
    RecommendationV4Response createRecommendation(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("clusterDefinitionName") String clusterDefinitionName, @QueryParam("credentialName") String credentialName,
            @QueryParam("region") String region, @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

}
