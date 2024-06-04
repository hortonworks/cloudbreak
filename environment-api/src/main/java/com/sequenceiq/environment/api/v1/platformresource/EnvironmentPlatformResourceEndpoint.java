package com.sequenceiq.environment.api.v1.platformresource;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CONNECTOR_NOTES;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sequenceiq.cloudbreak.jerseyclient.RetryAndMetrics;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription.OpEnvDescription;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformPrivateDnsZonesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RetryAndMetrics
@Path("/v1/env/platform_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "/v1/env/platform_resources", description = PlatformResourceModelDescription.CONNECTOR_V1_DESCRIPTION)
public interface EnvironmentPlatformResourceEndpoint {

    @GET
    @Path("machine_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_VMTYPES_BY_CREDENTIAL, description = CONNECTOR_NOTES,
            operationId = "getVmTypesByCredentialByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformVmtypesResponse getVmTypesByCredential(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("resourceType") CdpResourceType resourceType);

    @GET
    @Path("machine_types_for_vertical_scaling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_VERTICAL_SCALE_RECOMMENDATION, description = CONNECTOR_NOTES,
            operationId = "getVmTypesForVerticalScaling",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformVmtypesResponse getVmTypesForVerticalScaling(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("instanceType") String instanceType,
            @QueryParam("resourceType") CdpResourceType resourceType,
            @QueryParam("availabilityZones") List<String> availabilityZones);

    @GET
    @Path("regions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_REGIONS_BY_ENVIRONMENT, description = CONNECTOR_NOTES,
            operationId = "getRegionsByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    RegionResponse getRegionsByCredential(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @DefaultValue("true") @QueryParam("availabilityZonesNeeded") boolean availabilityZonesNeeded);

    @GET
    @Path("networks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_NETWORKS, description = CONNECTOR_NOTES,
            operationId = "getPlatformNetworksByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformNetworksResponse getCloudNetworks(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("sharedProjectId") String sharedProjectId);

    @GET
    @Path("ip_pools")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_IPPOOLS, description = CONNECTOR_NOTES,
            operationId = "getIpPoolsByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformIpPoolsResponse getIpPoolsCredentialId(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("gateways")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_GATEWAYS, description = CONNECTOR_NOTES,
            operationId = "getGatewaysByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformGatewaysResponse getGatewaysCredentialId(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("encryption_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_ENCRYPTIONKEYS, description = CONNECTOR_NOTES,
            operationId = "getEncryptionKeysByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformEncryptionKeysResponse getEncryptionKeys(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("security_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_SECURITYGROUPS, description = CONNECTOR_NOTES,
            operationId = "getPlatformSecurityGroupsByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformSecurityGroupsResponse getSecurityGroups(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("sharedProjectId") String sharedProjectId);

    @GET
    @Path("ssh_keys")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_SSHKEYS, description = CONNECTOR_NOTES,
            operationId = "getPlatformSShKeysByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformSshKeysResponse getCloudSshKeys(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("access_configs")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_ACCESSCONFIGS, description = CONNECTOR_NOTES,
            operationId = "getAccessConfigsByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformAccessConfigsResponse getAccessConfigs(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("accessConfigType") @DefaultValue("INSTANCE_PROFILE") AccessConfigTypeQueryParam accessConfigType);

    @GET
    @Path("nosql_tables")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_NOSQL_TABLES, description = CONNECTOR_NOTES,
            operationId = "getNoSqlTablesByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformNoSqlTablesResponse getNoSqlTables(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") @NotEmpty String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("resource_groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_RESOURCE_GROUPS, description = CONNECTOR_NOTES,
            operationId = "getResourceGroupsByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformResourceGroupsResponse getResourceGroups(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone);

    @GET
    @Path("private_dns_zones")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_PRIVATE_DNS_ZONES, description = CONNECTOR_NOTES,
            operationId = "getPrivateDnsZonesByEnv",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformPrivateDnsZonesResponse getPrivateDnsZones(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("platformVariant") String platformVariant);

    @GET
    @Path("database_capabilities")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = OpEnvDescription.GET_DATABASE_CAPABILITIES, description = CONNECTOR_NOTES, operationId = "getDatabaseCapabilities",
            responses = @ApiResponse(responseCode = "200", description = "successful operation", useReturnTypeSchema = true))
    PlatformDatabaseCapabilitiesResponse getDatabaseCapabilities(
            @QueryParam("environmentCrn") @NotEmpty String environmentCrn,
            @QueryParam("region") String region,
            @QueryParam("platformVariant") String platformVariant,
            @QueryParam("availabilityZone") String availabilityZone,
            @QueryParam("databaseType") DatabaseCapabilityType databaseType);

}
