package com.sequenceiq.environment.api.v1.proxy.endpoint;

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

import com.sequenceiq.environment.api.proxy.doc.ProxyConfigDescription;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/proxies")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/proxies", protocols = "http,https")
public interface ProxyEndpoint {

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.LIST, produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "listProxyConfigsV1")
    ProxyResponses list();

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "getProxyConfigV1")
    ProxyResponse get(@PathParam("name") String name);

    @POST
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.CREATE, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "createProxyConfigV1")
    ProxyResponse post(@Valid ProxyRequest request);

    @DELETE
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "deleteProxyConfigV1")
    ProxyResponse delete(@PathParam("name") String name);

    @DELETE
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.DELETE_MULTIPLE_BY_NAME, produces = MediaType.APPLICATION_JSON,
            notes = ProxyConfigDescription.PROXY_CONFIG_NOTES, nickname = "deleteProxyConfigsV1")
    ProxyResponses deleteMultiple(Set<String> names);

    @GET
    @Path("{name}/request")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigDescription.GET_REQUEST_BY_NAME,
            produces = MediaType.APPLICATION_JSON, notes = ProxyConfigDescription.PROXY_CONFIG_NOTES,
            nickname = "getProxyRequestFromNameV1")
    ProxyRequest getRequest(@PathParam("name") String name);
}
