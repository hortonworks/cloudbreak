package com.sequenceiq.cloudbreak.api.endpoint.v3;

import com.sequenceiq.cloudbreak.api.model.PlatformAccessConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVmtypesResponse;
import com.sequenceiq.cloudbreak.api.model.RecommendationRequestJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;
import com.sequenceiq.cloudbreak.api.model.RegionResponse;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v3/{organizationId}/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v3/{organizationId}/connectors", description = ControllerDescription.CONNECTOR_V3_DESCRIPTION, protocols = "http,https")
public interface ConnectorV3Endpoint {

    @POST
    @Path("vmtypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_VMTYPES_BY_CREDENTIAL, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypesByCredentialAndOrganization")
    PlatformVmtypesResponse getVmTypesByCredential(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionsByCredentialAndOrganization")
    RegionResponse getRegionsByCredential(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @GET
    @Path("disktypes")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_DISK_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypesForOrganization")
    PlatformDisksJson getDisktypes(@PathParam("organizationId") Long organizationId);

    @POST
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_NETWORKS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformNetworksForOrganization")
    PlatformNetworksResponse getCloudNetworks(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("ippools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_IPPOOLS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getIpPoolsCredentialIdForOrganization")
    PlatformIpPoolsResponse getIpPoolsCredentialId(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_GATEWAYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getGatewaysCredentialIdForOrganization")
    PlatformGatewaysResponse getGatewaysCredentialId(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("encryptionkeys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ENCRYPTIONKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getEncryptionKeysForOrganization")
    PlatformEncryptionKeysResponse getEncryptionKeys(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendationForOrganization")
    RecommendationResponse createRecommendation(@PathParam("organizationId") Long organizationId, RecommendationRequestJson recommendationRequestJson);

    @POST
    @Path("securitygroups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SECURITYGROUPS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroupsForOrganization")
    PlatformSecurityGroupsResponse getSecurityGroups(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("sshkeys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SSHKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSShKeysForOrganization")
    PlatformSshKeysResponse getCloudSshKeys(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);

    @POST
    @Path("accessconfigs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ACCESSCONFIGS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getAccessConfigsForOrganization")
    PlatformAccessConfigsResponse getAccessConfigs(@PathParam("organizationId") Long organizationId, PlatformResourceRequestJson resourceRequestJson);
}
