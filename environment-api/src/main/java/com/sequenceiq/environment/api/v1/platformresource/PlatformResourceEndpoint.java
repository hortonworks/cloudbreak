package com.sequenceiq.environment.api.v1.platformresource;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CONNECTOR_NOTES;

import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.retry.RetryingRestClient;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription.OpDescription;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDisksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.TagSpecificationsResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryingRestClient
@Path("/v1/platform_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/platform_resources", description = PlatformResourceModelDescription.CONNECTOR_V1_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface PlatformResourceEndpoint {

    @GET
    @Path("machine_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_VMTYPES_BY_CREDENTIAL, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getVmTypesByCredential")
    PlatformVmtypesResponse getVmTypesByCredential(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_REGION_R_BY_TYPE, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getRegionsByCredential")
    RegionResponse getRegionsByCredential(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("disk_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_DISK_TYPES, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getDisktypes")
    PlatformDisksResponse getDisktypes();

    @GET
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_NETWORKS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getPlatformNetworks")
    PlatformNetworksResponse getCloudNetworks(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("ip_pools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_IPPOOLS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getIpPoolsCredentialId")
    PlatformIpPoolsResponse getIpPoolsCredentialId(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_GATEWAYS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getGatewaysCredentialId")
    PlatformGatewaysResponse getGatewaysCredentialId(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("encryption_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_ENCRYPTIONKEYS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getEncryptionKeys")
    PlatformEncryptionKeysResponse getEncryptionKeys(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("security_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_SECURITYGROUPS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroups")
    PlatformSecurityGroupsResponse getSecurityGroups(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("ssh_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_SSHKEYS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getPlatformSShKeys")
    PlatformSshKeysResponse getCloudSshKeys(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("access_configs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_ACCESSCONFIGS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getAccessConfigs")
    PlatformAccessConfigsResponse getAccessConfigs(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("accessConfigType") @DefaultValue("INSTANCE_PROFILE") AccessConfigTypeQueryParam accessConfigType);

    @GET
    @Path("tag_specifications")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_TAG_SPECIFICATIONS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getTagSpecifications")
    TagSpecificationsResponse getTagSpecifications();

    @GET
    @Path("nosql_tables")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpDescription.GET_NOSQL_TABLES, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getNoSqlTables")
    PlatformNoSqlTablesResponse getNoSqlTables(
            @QueryParam("credentialName") String credentialName,
            @QueryParam("credentialCrn") String credentialCrn,
            @QueryParam("region") @NotEmpty String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);
}
