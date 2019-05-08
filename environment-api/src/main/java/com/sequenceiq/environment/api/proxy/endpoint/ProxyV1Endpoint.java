package com.sequenceiq.environment.api.proxy.endpoint;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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

import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;
import com.sequenceiq.environment.api.proxy.model.EnvironmentNames;
import com.sequenceiq.environment.api.proxy.model.request.ProxyV1Request;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Responses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/proxies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/{workspaceId}/proxies", protocols = "http,https")
public interface ProxyV1Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.LIST_BY_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "listProxyConfigsByWorkspace")
    ProxyV1Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "getProxyConfigInWorkspace")
    ProxyV1Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.CREATE_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "createProxyConfigInWorkspace")
    ProxyV1Response post(@PathParam("workspaceId") Long workspaceId, @Valid ProxyV1Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "deleteProxyConfigInWorkspace")
    ProxyV1Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "deleteProxyConfigsInWorkspace")
    ProxyV1Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.ATTACH_TO_ENVIRONMENTS, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "attachProxyResourceToEnvironments")
    ProxyV1Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid @NotNull EnvironmentNames environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DETACH_FROM_ENVIRONMENTS, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "detachProxyResourceFromEnvironments")
    ProxyV1Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
            @Valid @NotNull EnvironmentNames environmentNames);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_REQUEST_BY_NAME,
            produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "getProxyRequestFromNameInWorkspace")
    ProxyV1Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
