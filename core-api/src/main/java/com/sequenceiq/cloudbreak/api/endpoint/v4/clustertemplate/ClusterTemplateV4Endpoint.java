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
import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Consumes(MediaType.APPLICATION_JSON)
@Path("/v4/{workspaceId}/cluster_templates")
@Api(value = "/v4/{workspaceId}/clustertemplates", description = ControllerDescription.CLUSTER_TEMPLATE_V4_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface ClusterTemplateV4Endpoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.CREATE_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "createClusterTemplateInWorkspace")
    ClusterTemplateV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid ClusterTemplateV4Request request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.LIST_BY_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "listClusterTemplatesByWorkspace")
    ClusterTemplateViewV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "getClusterTemplateByNameInWorkspace")
    ClusterTemplateV4Response getByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("name/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "deleteClusterTemplateByNameInWorkspace")
    ClusterTemplateV4Response deleteByName(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @GET
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_BY_CRN_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "getClusterTemplateByCrnInWorkspace")
    ClusterTemplateV4Response getByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @DELETE
    @Path("crn/{crn}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_BY_CRN_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "deleteClusterTemplateByCrnInWorkspace")
    ClusterTemplateV4Response deleteByCrn(@PathParam("workspaceId") Long workspaceId, @PathParam("crn") String crn);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON,
            notes = CLUSTER_TEMPLATE_NOTES, nickname = "deleteClusterTemplatesInWorkspace")
    ClusterTemplateV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names,
            @QueryParam("environmentName") String environmentName, @QueryParam("environmentCrn") String environmentCrn);

}
