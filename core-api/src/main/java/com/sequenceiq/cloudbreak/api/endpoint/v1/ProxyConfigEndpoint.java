package com.sequenceiq.cloudbreak.api.endpoint.v1;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ProxyConfigOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/proxyconfigs")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/proxyconfigs", description = ControllerDescription.PROXYCONFIG_DESCRIPTION, protocols = "http,https")
public interface ProxyConfigEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_BY_ID, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES, nickname = "getProxyConfig")
    ProxyV4Response get(@PathParam("id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_BY_ID, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES, nickname = "deleteProxyConfig")
    ProxyV4Response delete(@PathParam("id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.POST_PRIVATE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "postPrivateProxyConfig")
    ProxyV4Response postPrivate(@Valid ProxyV4Request proxyV4Request);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_PRIVATE, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getPrivatesProxyConfig")
    Set<ProxyV4Response> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getPrivateProxyConfig")
    ProxyV4Response getPrivate(@PathParam("name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_PRIVATE_BY_NAME, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deletePrivateProxyConfig")
    ProxyV4Response deletePrivate(@PathParam("name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.POST_PUBLIC, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "postPublicProxyConfig")
    ProxyV4Response postPublic(@Valid ProxyV4Request proxyV4Request);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_PUBLIC, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getPublicsProxyConfig")
    Set<ProxyV4Response> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.GET_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "getPublicProxyConfig")
    ProxyV4Response getPublic(@PathParam("name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ProxyConfigOpDescription.DELETE_PUBLIC_BY_NAME, produces = ContentType.JSON, notes = Notes.PROXY_CONFIG_NOTES,
            nickname = "deletePublicProxyConfig")
    ProxyV4Response deletePublic(@PathParam("name") String name);
}
