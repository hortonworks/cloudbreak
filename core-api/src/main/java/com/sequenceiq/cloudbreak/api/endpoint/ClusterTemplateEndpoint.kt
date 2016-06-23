package com.sequenceiq.cloudbreak.api.endpoint

import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse
import com.sequenceiq.cloudbreak.api.model.IdJson

@Path("/clustertemplates")
@Consumes(MediaType.APPLICATION_JSON)
interface ClusterTemplateEndpoint {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    operator fun get(@PathParam(value = "id") id: Long?): ClusterTemplateResponse

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun delete(@PathParam(value = "id") id: Long?)

    @POST
    @Path("user")
    @Produces(MediaType.APPLICATION_JSON)
    fun postPrivate(clusterTemplateRequest: ClusterTemplateRequest): IdJson

    val privates: Set<ClusterTemplateResponse>

    @GET
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPrivate(@PathParam(value = "name") name: String): ClusterTemplateResponse

    @DELETE
    @Path("user/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deletePrivate(@PathParam(value = "name") name: String)

    @POST
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    fun postPublic(clusterTemplateRequest: ClusterTemplateRequest): IdJson

    val publics: Set<ClusterTemplateResponse>

    @GET
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPublic(@PathParam(value = "name") name: String): ClusterTemplateResponse

    @DELETE
    @Path("account/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deletePublic(@PathParam(value = "name") name: String)

}
