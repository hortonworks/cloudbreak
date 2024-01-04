package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint;

import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/blueprints_util")
@Tag(name = "/v4/{workspaceId}/blueprints_util", description = ControllerDescription.BLUEPRINT_V4_DESCRIPTION)
public interface BlueprintUtilV4Endpoint {

    @GET
    @Path("service_dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION, description = Notes.CONNECTOR_NOTES,
            operationId = "getServiceAndDependencies",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ServiceDependencyMatrixV4Response getServiceAndDependencies(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("services")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION, description = Notes.CONNECTOR_NOTES,
            operationId = "getServiceList",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    SupportedVersionsV4Response getServiceList(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("generate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION, description = Notes.CONNECTOR_NOTES,
            operationId = "getGeneratedTemplate",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    GeneratedCmTemplateV4Response getGeneratedTemplate(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("services") Set<String> services, @QueryParam("platform") String platform);

    @GET
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION, description = Notes.CONNECTOR_NOTES,
            operationId = "createRecommendationForWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
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
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION_BY_CRED_CRN, description = Notes.CONNECTOR_NOTES,
            operationId = "createRecommendationForWorkspaceByCredCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
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
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION_BY_ENV_CRN, description = Notes.CONNECTOR_NOTES,
            operationId = "createRecommendationForWorkspaceByEnvCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
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
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION, description = Notes.CONNECTOR_NOTES,
            operationId = "createScaleRecommendationForWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ScaleRecommendationV4Response createRecommendation(
            @PathParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("blueprintName") @NotEmpty String blueprintName);

    @GET
    @Path("scalerecommendation_by_datahub_crn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ConnectorOpDescription.GET_RECOMMENDATION_BY_DATAHUB_CRN, description = Notes.CONNECTOR_NOTES,
            operationId = "createScaleRecommendationForWorkspaceByDatahubCrn",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ScaleRecommendationV4Response createRecommendationByDatahubCrn(
            @PathParam("workspaceId") @Valid Long workspaceId,
            @QueryParam("datahubCrn") @NotEmpty String datahubCrn);

    @GET
    @Path("service_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ConnectorOpDescription.GET_SERVICE_VERSIONS_BY_BLUEPRINT_NAME, description = Notes.CONNECTOR_NOTES,
            operationId = "getServiceVersionsByBlueprintName",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    BlueprintServicesV4Response getServicesByBlueprint(@PathParam("workspaceId") Long workspaceId,
        @QueryParam("blueprintName") String blueprintName);

}
