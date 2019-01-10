package com.sequenceiq.cloudbreak.api.endpoint.v4.connector;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.requests.PlatformResourceV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformGatewaysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformIpPoolsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.cloudbreak.api.model.RecommendationV4Request;
import com.sequenceiq.cloudbreak.api.model.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.model.RegionV4Response;
import com.sequenceiq.cloudbreak.doc.ContentType;
import com.sequenceiq.cloudbreak.doc.ControllerDescription;
import com.sequenceiq.cloudbreak.doc.Notes;
import com.sequenceiq.cloudbreak.doc.OperationDescriptions.ConnectorOpDescription;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v4/{workspaceId}/connectors")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v4/{workspaceId}/connectors", description = ControllerDescription.CONNECTOR_V4_DESCRIPTION, protocols = "http,https")
public interface ConnectorV4Endpoint {

    @POST
    @Path("machine_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_VMTYPES_BY_CREDENTIAL, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypesByCredentialAndWorkspace")
    PlatformVmtypesV4Response getVmTypesByCredential(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionsByCredentialAndWorkspace")
    RegionV4Response getRegionsByCredential(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @GET
    @Path("disk_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_DISK_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypesForWorkspace")
    PlatformDisksV4Response getDisktypes(@PathParam("workspaceId") Long workspaceId);

    @POST
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_NETWORKS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformNetworksForWorkspace")
    PlatformNetworksV4Response getCloudNetworks(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("ip_pools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_IPPOOLS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getIpPoolsCredentialIdForWorkspace")
    PlatformIpPoolsV4Response getIpPoolsCredentialId(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_GATEWAYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getGatewaysCredentialIdForWorkspace")
    PlatformGatewaysV4Response getGatewaysCredentialId(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("encryption_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ENCRYPTIONKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getEncryptionKeysForWorkspace")
    PlatformEncryptionKeysV4Response getEncryptionKeys(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("recommendation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_RECOMMENDATION, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "createRecommendationForWorkspace")
    RecommendationV4Response createRecommendation(@PathParam("workspaceId") Long workspaceId, RecommendationV4Request recommendationV4Request);

    @POST
    @Path("security_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SECURITYGROUPS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroupsForWorkspace")
    PlatformSecurityGroupsV4Response getSecurityGroups(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("ssh_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SSHKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSShKeysForWorkspace")
    PlatformSshKeysV4Response getCloudSshKeys(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);

    @POST
    @Path("access_configs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ACCESSCONFIGS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getAccessConfigsForWorkspace")
    PlatformAccessConfigsV4Response getAccessConfigs(@PathParam("workspaceId") Long workspaceId, PlatformResourceV4Request resourceRequestJson);
}
