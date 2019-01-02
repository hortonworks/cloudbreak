package com.sequenceiq.cloudbreak.api.endpoint.v4.connector;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.filters.PlatformResourceV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformGatewaysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformIpPoolsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.TagSpecificationsV4Response;
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

    @GET
    @Path("machine_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_VMTYPES_BY_CREDENTIAL, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getVmTypesByCredentialAndWorkspace")
    PlatformVmtypesV4Response getVmTypesByCredential(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_REGION_R_BY_TYPE, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getRegionsByCredentialAndWorkspace")
    RegionV4Response getRegionsByCredential(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("disk_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_DISK_TYPES, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getDisktypesForWorkspace")
    PlatformDisksV4Response getDisktypes(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_NETWORKS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformNetworksForWorkspace")
    PlatformNetworksV4Response getCloudNetworks(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("ip_pools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_IPPOOLS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getIpPoolsCredentialIdForWorkspace")
    PlatformIpPoolsV4Response getIpPoolsCredentialId(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_GATEWAYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getGatewaysCredentialIdForWorkspace")
    PlatformGatewaysV4Response getGatewaysCredentialId(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("encryption_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ENCRYPTIONKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getEncryptionKeysForWorkspace")
    PlatformEncryptionKeysV4Response getEncryptionKeys(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("security_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SECURITYGROUPS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroupsForWorkspace")
    PlatformSecurityGroupsV4Response getSecurityGroups(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("ssh_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_SSHKEYS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getPlatformSShKeysForWorkspace")
    PlatformSshKeysV4Response getCloudSshKeys(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("access_configs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_ACCESSCONFIGS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getAccessConfigsForWorkspace")
    PlatformAccessConfigsV4Response getAccessConfigs(@PathParam("workspaceId") Long workspaceId, @BeanParam PlatformResourceV4Filter resourceRequestJson);

    @GET
    @Path("tag_specifications")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = ConnectorOpDescription.GET_TAG_SPECIFICATIONS, produces = ContentType.JSON, notes = Notes.CONNECTOR_NOTES,
            nickname = "getTagSpecifications")
    TagSpecificationsV4Response getTagSpecifications(@PathParam("workspaceId") Long workspaceId);
}
