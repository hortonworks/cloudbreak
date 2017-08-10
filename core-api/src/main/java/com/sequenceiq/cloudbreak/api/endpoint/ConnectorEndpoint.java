package com.sequenceiq.cloudbreak.api.endpoint;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformImagesJson;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.TagSpecificationsJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/connectors", description = ControllerDescription.CONNECTOR_DESCRIPTION, protocols = "http,https")
public interface ConnectorEndpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_PLATFORMS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatforms")
    Map<String, JsonEntity> getPlatforms(@QueryParam("extended") Boolean extended);

    @GET
    @Path("variants")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_PLATFORM_VARIANTS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformVariants")
    PlatformVariantsJson getPlatformVariants();

    @GET
    @Path(value = "variants/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_PLATFORM_VARIANT_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformVariantByType")
    Collection<String> getPlatformVariantByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "disktypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_DISK_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypes")
    PlatformDisksJson getDisktypes();

    @GET
    @Path(value = "disktypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_DISK_TYPE_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypeByType")
    Collection<String> getDisktypeByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "ochestrators")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_ORCHESTRATOR_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getOrchestratortypes")
    PlatformOrchestratorsJson getOrchestratortypes();

    @GET
    @Path(value = "ochestrators/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_ORCHESTRATORS_BY_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getOchestratorsByType")
    Collection<String> getOchestratorsByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "connectors/vmtypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_VM_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypes")
    PlatformVirtualMachinesJson getVmTypes(@QueryParam("extended") Boolean extended);

    @GET
    @Path(value = "vmtypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_VM_TYPE_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypeByType")
    Collection<VmTypeJson> getVmTypeByType(@PathParam(value = "type") String type, @QueryParam("extended") Boolean extended);

    @GET
    @Path(value = "connectors/regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_REGIONS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegions")
    PlatformRegionsJson getRegions();

    @GET
    @Path(value = "regions/r/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionRByType")
    Collection<String> getRegionRByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "regions/av/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_REGION_AV_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionAvByType")
    Map<String, Collection<String>> getRegionAvByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "images/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_IMAGE_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getImagesByType")
    Map<String, String> getImagesByType(@PathParam(value = "type") String type);

    @GET
    @Path(value = "images")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_IMAGES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getImages")
    PlatformImagesJson getImages();

    @GET
    @Path(value = "tagspecifications")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_TAG_SPECIFICATIONS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getTagSpecifications")
    TagSpecificationsJson getTagSpecifications();

    @GET
    @Path(value = "custom")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_SPECIALS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getSpecialProperties")
    Map<String, Boolean> getSpecialProperties();

    @POST
    @Path(value = "networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_NETWORKS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformNetworks")
    PlatformNetworksResponse getCloudNetworks(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path(value = "sshkeys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_SSHKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSShKeys")
    PlatformSshKeysResponse getCloudSshKeys(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path(value = "securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OperationDescriptions.ConnectorOpDescription.GET_SECURITYGROUPS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroups")
    PlatformSecurityGroupsResponse getSecurityGroups(PlatformResourceRequestJson resourceRequestJson);


}
