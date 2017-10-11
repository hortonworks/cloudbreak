package com.sequenceiq.cloudbreak.api.endpoint.v1;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformImagesJson;
import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworkResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeyResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationRequestJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;
import com.sequenceiq.cloudbreak.api.model.TagSpecificationsJson;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/connectors", description = ControllerDescription.CONNECTOR_DESCRIPTION, protocols = "http,https")
public interface ConnectorV1Endpoint {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_PLATFORMS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatforms")
    Map<String, Object> getPlatforms(@QueryParam("extended") Boolean extended);

    @GET
    @Path("variants")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_PLATFORM_VARIANTS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformVariants")
    PlatformVariantsJson getPlatformVariants();

    @GET
    @Path("variants/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_PLATFORM_VARIANT_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformVariantByType")
    Collection<String> getPlatformVariantByType(@PathParam("type") String type);

    @GET
    @Path("disktypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_DISK_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypes")
    PlatformDisksJson getDisktypes();

    @GET
    @Path("disktypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_DISK_TYPE_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypeByType")
    Collection<String> getDisktypeByType(@PathParam("type") String type);

    @GET
    @Path("ochestrators")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ORCHESTRATOR_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getOrchestratortypes")
    PlatformOrchestratorsJson getOrchestratortypes();

    @GET
    @Path("ochestrators/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ORCHESTRATORS_BY_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getOchestratorsByType")
    Collection<String> getOchestratorsByType(@PathParam("type") String type);

    @GET
    @Path("vmtypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_VM_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypes")
    PlatformVirtualMachinesJson getVmTypes(@QueryParam("extended") Boolean extended);

    @GET
    @Path("vmtypes/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_VM_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypesByType")
    PlatformVirtualMachinesJson getVmTypes(@PathParam("type") String type, @QueryParam("extended") Boolean extended);

    @GET
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGIONS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegions")
    PlatformRegionsJson getRegions();

    @GET
    @Path("regions/r/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionRByType")
    Collection<String> getRegionRByType(@PathParam("type") String type);

    @GET
    @Path("regions/av/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGION_AV_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionAvByType")
    Map<String, Collection<String>> getRegionAvByType(@PathParam("type") String type);

    @GET
    @Path("images/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_IMAGE_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getImagesByType")
    Map<String, String> getImagesByType(@PathParam("type") String type);

    @GET
    @Path("images")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_IMAGES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getImages")
    PlatformImagesJson getImages();

    @GET
    @Path("tagspecifications")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_TAG_SPECIFICATIONS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getTagSpecifications")
    TagSpecificationsJson getTagSpecifications();

    @POST
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendation")
    RecommendationResponse createRecommendation(RecommendationRequestJson recommendationRequestJson);

    @POST
    @Path("custom")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SPECIALS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getSpecialProperties")
    Map<String, Boolean> getSpecialProperties();

    @POST
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_NETWORKS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformNetworks")
    Map<String, Set<PlatformNetworkResponse>> getCloudNetworks(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("sshkeys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SSHKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSShKeys")
    Map<String, Set<PlatformSshKeyResponse>> getCloudSshKeys(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SECURITYGROUPS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroups")
    Map<String, Set<PlatformSecurityGroupResponse>> getSecurityGroups(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_GATEWAYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getGatewaysCredentialId")
    PlatformGatewaysResponse getGatewaysCredentialId(PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("ippools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_IPPOOLS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getIpPoolsCredentialId")
    PlatformIpPoolsResponse getIpPoolsCredentialId(PlatformResourceRequestJson resourceRequestJson);
}
