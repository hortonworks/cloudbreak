package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.CLUSTER_TEMPLATE_NOTES;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Responses;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ClusterTemplateOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.Set;

@Path("/v4/{workspaceId}/cluster_templates")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/clustertemplates", description = ControllerDescription.CLUSTER_TEMPLATE_V4_DESCRIPTION, protocols = "http,https")
public interface ClusterTemplateV4Endpoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.CREATE_IN_WORKSPACE, produces = JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "createClusterTemplateInWorkspace")
    ClusterTemplateV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid ClusterTemplateV4Request request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "listClusterTemplatesByWorkspace")
    ClusterTemplateViewV4Responses list(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "getClusterTemplateInWorkspace")
    ClusterTemplateV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "deleteClusterTemplateInWorkspace")
    ClusterTemplateV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ClusterTemplateOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = CLUSTER_TEMPLATE_NOTES,
            nickname = "deleteClusterTemplatesInWorkspace")
    ClusterTemplateV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);
}
