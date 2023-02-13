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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/blueprints_util")
@Tag(name = "/v4/{workspaceId}/blueprints_util", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION)
public interface BlueprintUtilV4Endpoint {

    @GET
    @Path("service_dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION, description =  Notes.CONNECTOR_NOTES,
            operationId = "getServiceAndDependencies")
    ServiceDependencyMatrixV4Response getServiceAndDependencies(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("services")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION, description =  Notes.CONNECTOR_NOTES,
            operationId = "getServiceList")
    SupportedVersionsV4Response getServiceList(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION, description =  Notes.CONNECTOR_NOTES,
            operationId = "getGeneratedTemplate")
    GeneratedCmTemplateV4Response getGeneratedTemplate(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION, description =  Notes.CONNECTOR_NOTES,
            operationId = "createRecommendationForWorkspace")
    RecommendationV4Response createRecommendation(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("definitionName") String definitionName,
            @QueryParam("blueprintName") String blueprintName,
            @QueryParam("credentialName") String credentialName,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("recommendation_by_cred_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION_BY_CRED_CRN, description =  Notes.CONNECTOR_NOTES,
            operationId = "createRecommendationForWorkspaceByCredCrn")
    RecommendationV4Response createRecommendationByCredCrn(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("definitionName") String definitionName,
            @QueryParam("blueprintName") String blueprintName,
            @QueryParam("credentialCrn") String credentialCrn,
            @NotEmpty @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("recommendation_by_env_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION_BY_ENV_CRN, description =  Notes.CONNECTOR_NOTES,
            operationId = "createRecommendationForWorkspaceByEnvCrn")
    RecommendationV4Response createRecommendationByEnvCrn(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("definitionName") String definitionName,
            @QueryParam("blueprintName") String blueprintName,
            @QueryParam("environmentCrn") String environmentCrn,
            @NotEmpty @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("scalerecommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION, description =  Notes.CONNECTOR_NOTES,
            operationId = "createScaleRecommendationForWorkspace")
    ScaleRecommendationV4Response createRecommendation(
            @PathParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("blueprintName") @NotEmpty String blueprintName);

    @GET
    @Path("scalerecommendation_by_datahub_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_RECOMMENDATION_BY_DATAHUB_CRN, description =  Notes.CONNECTOR_NOTES,
            operationId = "createScaleRecommendationForWorkspaceByDatahubCrn")
    ScaleRecommendationV4Response createRecommendationByDatahubCrn(
            @PathParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("datahubCrn") @NotEmpty String datahubCrn);

    @GET
    @Path("service_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary =  ConnectorOpDescription.GET_SERVICE_VERSIONS_BY_BLUEPRINT_NAME, description =  Notes.CONNECTOR_NOTES,
            operationId = "getServiceVersionsByBlueprintName")
    BlueprintServicesV4Response getServicesByBlueprint(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("blueprintName") String blueprintName);

}
