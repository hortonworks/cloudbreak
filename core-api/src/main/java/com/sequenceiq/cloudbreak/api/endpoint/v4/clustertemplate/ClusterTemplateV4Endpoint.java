package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate;

import static com.sequenceiq.cloudbreak.doc.Notes.CLUSTER_TEMPLATE_NOTES;

import java.util.Set;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterTemplateOpDescription;
import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/cluster_templates")
@Tag(name = "/v4/{workspaceId}/cluster_templates", description = ControllerDescription.CLUSTER_TEMPLATE_V4_DESCRIPTION)
public interface ClusterTemplateV4Endpoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.CREATE_IN_WORKSPACE, description = CLUSTER_TEMPLATE_NOTES,
            operationId = "createClusterTemplateInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid ClusterTemplateV4Request request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.LIST_BY_WORKSPACE, description = CLUSTER_TEMPLATE_NOTES,
            operationId = "listClusterTemplatesByWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateViewV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("env/{environmentCrn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.LIST_BY_ENV,
            description = CLUSTER_TEMPLATE_NOTES, operationId = "listClusterTemplatesByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateViewV4Responses listByEnv(@PathParam("workspaceId") Long workspaceId, @PathParam("environmentCrn") String environmentCrn);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.GET_BY_NAME_IN_WORKSPACE, description = CLUSTER_TEMPLATE_NOTES,
            operationId = "getClusterTemplateByNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateV4Response getByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.DELETE_BY_NAME_IN_WORKSPACE, description = CLUSTER_TEMPLATE_NOTES,
            operationId = "deleteClusterTemplateByNameInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateV4Response deleteByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.GET_BY_CRN_IN_WORKSPACE, description = CLUSTER_TEMPLATE_NOTES,
            operationId = "getClusterTemplateByCrnInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.DELETE_BY_CRN_IN_WORKSPACE, description = CLUSTER_TEMPLATE_NOTES,
            operationId = "deleteClusterTemplateByCrnInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateV4Response deleteByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = ClusterTemplateOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE,
            description = CLUSTER_TEMPLATE_NOTES, operationId = "deleteClusterTemplatesInWorkspace",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    ClusterTemplateV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names,
            @QueryParam("environmentName") String environmentName, @QueryParam("environmentCrn") String environmentCrn);

}
