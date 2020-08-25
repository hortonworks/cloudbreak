package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintServicesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.GeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.ServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.ScaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.type.CdpResourceType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/blueprints_util")
@Api(value = "/v4/{workspaceId}/blueprints_util", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface BlueprintUtilV4Endpoint {

    @GET
    @Path("service_dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getServiceAndDependencies")
    ServiceDependencyMatrixV4Response getServiceAndDependencies(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("services")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getServiceList")
    SupportedVersionsV4Response getServiceList(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getGeneratedTemplate")
    GeneratedCmTemplateV4Response getGeneratedTemplate(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendationForWorkspace")
    RecommendationV4Response createRecommendation(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("blueprintName") String blueprintName,
            @QueryParam("credentialName") String credentialName,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("recommendation_by_cred_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION_BY_CRED_CRN, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendationForWorkspaceByCredCrn")
    RecommendationV4Response createRecommendationByCredCrn(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("blueprintName") String blueprintName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("scalerecommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createScaleRecommendationForWorkspace")
    ScaleRecommendationV4Response createRecommendation(
            @PathParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("blueprintName") @NotEmpty String blueprintName);

    @GET
    @Path("service_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SERVICE_VERSIONS_BY_BLUEPRINT_NAME, produces = MediaType.APPLICATION_JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getServiceVersionsByBlueprintName")
    BlueprintServicesV4Response getServicesByBlueprint(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("blueprintName") String blueprintName);

}
