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

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription.OpEnvDescription;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RetryAndMetrics
@Path("/v1/env/platform_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Api(value = "/v1/env/platform_resources", description = PlatformResourceModelDescription.CONNECTOR_V1_DESCRIPTION, protocols = "http,https",
        consumes = MediaType.APPLICATION_JSON)
public interface EnvironmentPlatformResourceEndpoint {

    @GET
    @Path("machine_types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_VMTYPES_BY_CREDENTIAL, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getVmTypesByCredentialByEnv")
    PlatformVmtypesResponse getVmTypesByCredential(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_REGION_R_BY_TYPE, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getRegionsByEnv")
    RegionResponse getRegionsByCredential(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @DefaultValue("true") @QueryParam("availabilityZonesNeeded") boolean availabilityZonesNeeded);

    @GET
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_NETWORKS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getPlatformNetworksByEnv")
    PlatformNetworksResponse getCloudNetworks(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("sharedProjectId") String sharedProjectId);

    @GET
    @Path("ip_pools")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_IPPOOLS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getIpPoolsByEnv")
    PlatformIpPoolsResponse getIpPoolsCredentialId(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_GATEWAYS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getGatewaysByEnv")
    PlatformGatewaysResponse getGatewaysCredentialId(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("encryption_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_ENCRYPTIONKEYS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getEncryptionKeysByEnv")
    PlatformEncryptionKeysResponse getEncryptionKeys(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("security_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_SECURITYGROUPS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getPlatformSecurityGroupsByEnv")
    PlatformSecurityGroupsResponse getSecurityGroups(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("sharedProjectId") String sharedProjectId);

    @GET
    @Path("ssh_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_SSHKEYS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getPlatformSShKeysByEnv")
    PlatformSshKeysResponse getCloudSshKeys(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("access_configs")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_ACCESSCONFIGS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getAccessConfigsByEnv")
    PlatformAccessConfigsResponse getAccessConfigs(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("accessConfigType") @DefaultValue("INSTANCE_PROFILE") AccessConfigTypeQueryParam accessConfigType);

    @GET
    @Path("nosql_tables")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_NOSQL_TABLES, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getNoSqlTablesByEnv")
    PlatformNoSqlTablesResponse getNoSqlTables(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") @NotEmpty String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("resource_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = OpEnvDescription.GET_RESOURCE_GROUPS, produces = MediaType.APPLICATION_JSON, notes = CONNECTOR_NOTES,
            nickname = "getResourceGroupsByEnv")
    PlatformResourceGroupsResponse getResourceGroups(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);
}
