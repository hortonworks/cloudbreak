package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.IdJson;

@Path("/clustertemplates")
@Consumes(MediaType.APPLICATION_JSON)
public interface ClusterTemplateEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    ClusterTemplateResponse get(@PathParam(value = "id") Long id);

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    void delete(@PathParam(value = "id") Long id);

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    IdJson postPrivate(ClusterTemplateRequest clusterTemplateRequest);

    @GET
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    Set<ClusterTemplateResponse> getPrivates();

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    ClusterTemplateResponse getPrivate(@PathParam(value = "name") String name);

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    void deletePrivate(@PathParam(value = "name") String name);

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    IdJson postPublic(ClusterTemplateRequest clusterTemplateRequest);

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    Set<ClusterTemplateResponse> getPublics();

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    ClusterTemplateResponse getPublic(@PathParam(value = "name") String name);

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    void deletePublic(@PathParam(value = "name") String name);

}
