package com.sequenceiq.cloudbreak.api.endpoint

import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.sequenceiq.cloudbreak.api.model.JsonEntity
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson
import com.sequenceiq.cloudbreak.api.model.VmTypeJson

@Path("/connectors")
@Consumes(MediaType.APPLICATION_JSON)
interface ConnectorEndpoint {

    val platforms: Map<String, JsonEntity>

    val platformVariants: PlatformVariantsJson

    @GET
    @Path(value = "variants/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPlatformVariantByType(@PathParam(value = "type") type: String): Collection<String>

    val disktypes: PlatformDisksJson

    @GET
    @Path(value = "disktypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDisktypeByType(@PathParam(value = "type") type: String): Collection<String>

    val orchestratortypes: PlatformOrchestratorsJson

    @GET
    @Path(value = "ochestrators/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getOchestratorsByType(@PathParam(value = "type") type: String): Collection<String>

    val vmTypes: PlatformVirtualMachinesJson

    @GET
    @Path(value = "vmtypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getVmTypeByType(@PathParam(value = "type") type: String): Collection<VmTypeJson>

    val regions: PlatformRegionsJson

    @GET
    @Path(value = "regions/r/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegionRByType(@PathParam(value = "type") type: String): Collection<String>

    @GET
    @Path(value = "regions/av/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegionAvByType(@PathParam(value = "type") type: String): Map<String, Collection<String>>
}
