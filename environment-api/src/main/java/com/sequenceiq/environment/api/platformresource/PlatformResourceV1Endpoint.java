package com.sequenceiq.environment.api.platformresource;

import static com.sequenceiq.environment.api.platformresource.PlatformResourceModelDescription.JSON_CONTENT_TYPE;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.platformresource.PlatformResourceModelDescription.OpDescription;
import com.sequenceiq.environment.api.platformresource.model.PlatformAccessConfigsV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformDisksV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformEncryptionKeysV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformGatewaysV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformIpPoolsV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformNetworksV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformSecurityGroupsV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformSshKeysV1Response;
import com.sequenceiq.environment.api.platformresource.model.PlatformVmtypesV1Response;
import com.sequenceiq.environment.api.platformresource.model.RegionV1Response;
import com.sequenceiq.environment.api.platformresource.model.TagSpecificationsV1Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/v1/platform_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/platform_resources", description = PlatformResourceModelDescription.CONNECTOR_V1_DESCRIPTION, protocols = "http,https")
public interface PlatformResourceV1Endpoint {

    @GET
    @Path("machine_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_VMTYPES_BY_CREDENTIAL, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getVmTypesByCredentialAndWorkspace")
    PlatformVmtypesV1Response getVmTypesByCredential(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_REGION_R_BY_TYPE, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getRegionsByCredentialAndWorkspace")
    RegionV1Response getRegionsByCredential(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("disk_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_DISK_TYPES, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getDisktypesForWorkspace")
    PlatformDisksV1Response getDisktypes(@PathParam("workspaceId") Long workspaceId);

    @GET
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_NETWORKS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getPlatformNetworksForWorkspace")
    PlatformNetworksV1Response getCloudNetworks(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("ip_pools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_IPPOOLS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getIpPoolsCredentialIdForWorkspace")
    PlatformIpPoolsV1Response getIpPoolsCredentialId(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_GATEWAYS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getGatewaysCredentialIdForWorkspace")
    PlatformGatewaysV1Response getGatewaysCredentialId(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("encryption_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_ENCRYPTIONKEYS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getEncryptionKeysForWorkspace")
    PlatformEncryptionKeysV1Response getEncryptionKeys(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("security_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_SECURITYGROUPS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroupsForWorkspace")
    PlatformSecurityGroupsV1Response getSecurityGroups(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("ssh_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_SSHKEYS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getPlatformSShKeysForWorkspace")
    PlatformSshKeysV1Response getCloudSshKeys(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("access_configs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_ACCESSCONFIGS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getAccessConfigsForWorkspace")
    PlatformAccessConfigsV1Response getAccessConfigs(@PathParam("workspaceId") Long workspaceId,
            @QueryParam("credentialName") String credentialName, @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant, @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("tag_specifications")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_TAG_SPECIFICATIONS, produces = JSON_CONTENT_TYPE, notes = ModelDescriptions.CONNECTOR_NOTES,
            nickname = "getTagSpecifications")
    TagSpecificationsV1Response getTagSpecifications(@PathParam("workspaceId") Long workspaceId);
}
