package com.sequenceiq.cloudbreak.api.endpoint.v3;

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

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ProxyConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v3/{organizationId}/proxyconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/proxyconfigs", description = ControllerDescription.RECIPE_V3_DESCRIPTION, protocols = "http,https")
public interface ProxyConfigV3Endpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.LIST_BY_ORGANIZATION, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "listProxyconfigsByOrganization")
    Set<ProxyConfigResponse> listByOrganization(@PathParam("organizationId") Long organizationId);

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getProxyconfigInOrganization")
    ProxyConfigResponse getByNameInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.CREATE_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "createProxyconfigInOrganization")
    ProxyConfigResponse createInOrganization(@PathParam("organizationId") Long organizationId, @Valid ProxyConfigRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_BY_NAME_IN_ORG, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deleteProxyconfigInOrganization")
    ProxyConfigResponse deleteInOrganization(@PathParam("organizationId") Long organizationId, @PathParam("name") String name);

}
