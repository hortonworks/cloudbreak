package com.sequenceiq.cloudbreak.api.endpoint.v4.proxies;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Responses;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ProxyConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/proxies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/proxies", description = ControllerDescription.PROXY_CONFIG_V3_DESCRIPTION, protocols = "http,https")
public interface ProxyV4Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.LIST_BY_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "listProxyConfigsByWorkspace")
    ProxyV4Responses list(@PathParam("workspaceId") Long workspaceId, @QueryParam("environment") String environment,
            @QueryParam("attachGlobal") Boolean attachGlobal);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getProxyConfigInWorkspace")
    ProxyV4Response get(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.CREATE_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "createProxyConfigInWorkspace")
    ProxyV4Response post(@PathParam("workspaceId") Long workspaceId, @Valid ProxyV4Request request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deleteProxyConfigInWorkspace")
    ProxyV4Response delete(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_MULTIPLE_BY_NAME_IN_WORKSPACE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deleteProxyConfigsInWorkspace")
    ProxyV4Responses deleteMultiple(@PathParam("workspaceId") Long workspaceId, Set<String> names);

    @PUT
    @Path("{name}/attach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.ATTACH_TO_ENVIRONMENTS, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "attachProxyResourceToEnvironments")
    ProxyV4Response attach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);

    @PUT
    @Path("{name}/detach")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DETACH_FROM_ENVIRONMENTS, produces = ContentType.JSON,
            notes = Notes.PROXY_CONFIG_NOTES, nickname = "detachProxyResourceFromEnvironments")
    ProxyV4Response detach(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name,
        @Valid @NotNull EnvironmentNames environmentNames);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_REQUEST_BY_NAME,
            produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getProxyRequestFromNameInWorkspace")
    ProxyV4Request getRequest(@PathParam("workspaceId") Long workspaceId, @PathParam("name") String name);
}
