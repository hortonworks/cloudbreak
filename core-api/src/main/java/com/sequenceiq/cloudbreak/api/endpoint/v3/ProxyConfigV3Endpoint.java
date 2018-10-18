package com.sequenceiq.cloudbreak.api.endpoint.v3;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ProxyConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{workspaceId}/proxyconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{workspaceId}/proxyconfigs", description = ControllerDescription.PROXY_CONFIG_V3_DESCRIPTION, protocols = "http,https")
public interface ProxyConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "listProxyconfigsByWorkspace")
    Set<ProxyConfigResponse> listByWorkspace(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getProxyconfigInWorkspace")
    ProxyConfigResponse getByNameInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "createProxyconfigInWorkspace")
    ProxyConfigResponse createInWorkspace(@PathParam("workspaceId") Long workspaceId, @Valid ProxyConfigRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deleteProxyconfigInWorkspace")
    ProxyConfigResponse deleteInWorkspace(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "attachProxyResourceToEnvironments")
    ProxyConfigResponse attachToEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON,
            notes = Notes.PROXY_CONFIG_NOTES, nickname = "detachProxyResourceFromEnvironments")
    ProxyConfigResponse detachFromEnvironments(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @NotEmpty Set<String> environmentNames);
}
