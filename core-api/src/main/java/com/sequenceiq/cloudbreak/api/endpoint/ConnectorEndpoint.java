package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;

@Path("/connectors")
@Consumes(MediaType.APPLICATION_JSON)
public interface ConnectorEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, JsonEntity> getPlatforms(@QueryParam("extended") Boolean extended);

    @GET
    @Path("variants")
    @Produces(MediaType.APPLICATION_JSON)
    PlatformVariantsJson getPlatformVariants();

    @GET
    @Path(value = "variants/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<String> getPlatformVariantByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "disktypes")
    @Produces(MediaType.APPLICATION_JSON)
    PlatformDisksJson getDisktypes();

    @GET
    @Path(value = "disktypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<String> getDisktypeByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "ochestrators")
    @Produces(MediaType.APPLICATION_JSON)
    PlatformOrchestratorsJson getOrchestratortypes();

    @GET
    @Path(value = "ochestrators/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<String> getOchestratorsByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "connectors/vmtypes")
    @Produces(MediaType.APPLICATION_JSON)
    PlatformVirtualMachinesJson getVmTypes(@QueryParam("extended") Boolean extended);

    @GET
    @Path(value = "vmtypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<VmTypeJson> getVmTypeByType(@PathParam(value = "type") String type, @QueryParam("extended") Boolean extended);

    @GET
    @Path(value = "connectors/regions")
    @Produces(MediaType.APPLICATION_JSON)
    PlatformRegionsJson getRegions();

    @GET
    @Path(value = "regions/r/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    Collection<String> getRegionRByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "regions/av/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Collection<String>> getRegionAvByType(@PathParam(value = "type") String type);
}
