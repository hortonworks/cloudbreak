package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/connectors", description = ControllerDescription.CONNECTOR_DESCRIPTION)
public interface ConnectorEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_PLATFORMS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Map<String, JsonEntity> getPlatforms();

    @GET
    @Path("variants")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_PLATFORM_VARIANTS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    PlatformVariantsJson getPlatformVariants();

    @GET
    @Path(value = "variants/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_PLATFORM_VARIANT_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Collection<String> getPlatformVariantByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "disktypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_DISK_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    PlatformDisksJson getDisktypes();

    @GET
    @Path(value = "disktypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_DISK_TYPE_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Collection<String> getDisktypeByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "ochestrators")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_ORCHESTRATOR_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    PlatformOrchestratorsJson getOrchestratortypes();

    @GET
    @Path(value = "ochestrators/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_ORCHESTRATORS_BY_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Collection<String> getOchestratorsByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "connectors/vmtypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_VM_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    PlatformVirtualMachinesJson getVmTypes();

    @GET
    @Path(value = "vmtypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_VM_TYPE_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Collection<VmTypeJson> getVmTypeByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "connectors/regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_REGIONS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    PlatformRegionsJson getRegions();

    @GET
    @Path(value = "regions/r/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Collection<String> getRegionRByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "regions/av/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_REGION_AV_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES)
    Map<String, Collection<String>> getRegionAvByType(@PathParam(value = "type") String type);
}
