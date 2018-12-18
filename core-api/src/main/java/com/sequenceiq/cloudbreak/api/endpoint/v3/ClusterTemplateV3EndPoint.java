package com.sequenceiq.cloudbreak.api.endpoint.v3;

import static com.sequenceiq.cloudbreak.doc.ContentType.JSON;
import static com.sequenceiq.cloudbreak.doc.Notes.TEMPLATE_NOTES;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/clustertemplate")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/clustertemplate", description = ControllerDescription.CLUSTERTEMPLATE_V3_DESCRIPTION, protocols = "http,https")
public interface ClusterTemplateV3EndPoint {
    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.CREATE_IN_WORKSPACE, produces = JSON, notes = TEMPLATE_NOTES,
            nickname = "createClusterTemplateInWorkspace")
    ClusterTemplateResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid ClusterTemplateRequest request);

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.LIST_BY_WORKSPACE, produces = JSON, notes = TEMPLATE_NOTES,
            nickname = "listClusterTemplatesByWorkspace")
    Set<ClusterTemplateResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = JSON, notes = TEMPLATE_NOTES,
            nickname = "getClusterTemplateInWorkspace")
    ClusterTemplateResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ClusterTemplateOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = JSON, notes = TEMPLATE_NOTES,
            nickname = "deleteClusterTemplateInWorkspace")
    void deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
