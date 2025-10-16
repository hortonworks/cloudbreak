package com.sequenceiq.environment.platformresource.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.ws.rs.BadRequestException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformPrivateDnsZoneResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformPrivateDnsZonesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.environment.platformresource.v1.converter.CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudGatewayssToPlatformGatewaysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudIpPoolsToPlatformIpPoolsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudNetworksToPlatformNetworksV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudSshKeysToPlatformSshKeysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudVmTypesToPlatformVmTypesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformRegionsToRegionV1ResponseConverter;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentPlatformResourceController implements EnvironmentPlatformResourceEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentPlatformResourceController.class);

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private PlatformRegionsToRegionV1ResponseConverter platformRegionsToRegionV1ResponseConverter;

    @Inject
    private CloudVmTypesToPlatformVmTypesV1ResponseConverter cloudVmTypesToPlatformVmTypesV1ResponseConverter;

    @Inject
    private CloudNetworksToPlatformNetworksV1ResponseConverter cloudNetworksToPlatformNetworksV1ResponseConverter;

    @Inject
    private CloudIpPoolsToPlatformIpPoolsV1ResponseConverter cloudIpPoolsToPlatformIpPoolsV1ResponseConverter;

    @Inject
    private CloudGatewayssToPlatformGatewaysV1ResponseConverter cloudGatewayssToPlatformGatewaysV1ResponseConverter;

    @Inject
    private CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter cloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter;

    @Inject
    private CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter cloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter;

    @Inject
    private CloudSshKeysToPlatformSshKeysV1ResponseConverter cloudSshKeysToPlatformSshKeysV1ResponseConverter;

    @Inject
    private CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter cloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter;

    @Inject
    private CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter;

    @Inject
    private DatabaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter databaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter;

    @Inject
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentService environmentService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformVmtypesResponse getVmTypesByCredential(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String architecture,
            CdpResourceType cdpResourceType) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                cdpResourceType);
        List<String> availabilityZones = new ArrayList<>();
        if (!Strings.isNullOrEmpty(availabilityZone)) {
            availabilityZones.add(availabilityZone);
        }
        setFilterForVmTypes(availabilityZones, architecture, request, accountId);
        LOGGER.info("Get /platform_resources/machine_types, request: {}", request);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        PlatformVmtypesResponse response = cloudVmTypesToPlatformVmTypesV1ResponseConverter.convert(cloudVmTypes);
        LOGGER.info("Resp /platform_resources/machine_types, request: {}, cloudVmTypes: {}, response: {}", request, cloudVmTypes, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformVmtypesResponse getVmTypesForVerticalScaling(
            @ResourceCrn String environmentCrn,
            String instanceType,
            CdpResourceType cdpResourceType,
            List<String> availabilityZones,
            String architecture) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        EnvironmentDto environmentDto = environmentService.getByCrnAndAccountId(environmentCrn, accountId);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironmentForVerticalScaling(
                accountId,
                environmentCrn,
                environmentDto.getRegions().stream().findFirst().get().getName(),
                cdpResourceType);
        setFilterForVmTypes(availabilityZones, architecture, request, accountId);
        LOGGER.debug("Get /platform_resources/machine_types_for_vertical_scaling, request: {}", request);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        cloudVmTypes = verticalScaleInstanceProvider.listInstanceTypes(
                null,
                instanceType,
                cloudVmTypes,
                null,
                cdpResourceType);
        PlatformVmtypesResponse response = cloudVmTypesToPlatformVmTypesV1ResponseConverter.convert(cloudVmTypes);
        LOGGER.debug("Resp /platform_resources/machine_types_for_vertical_scaling, request: {}, cloudVmTypes: {}, response: {}",
                request, cloudVmTypes, response);
        return response;
    }

    private void setFilterForVmTypes(List<String> availabilityZones, String architecture, PlatformResourceRequest request, String accountId) {
        Map<String, String> filter = new HashMap<>();
        if (CollectionUtils.isNotEmpty(availabilityZones)) {
            LOGGER.debug("Setting filter for Availability Zones {}", availabilityZones);
            filter.put(NetworkConstants.AVAILABILITY_ZONES, String.join(",", availabilityZones));
        }
        Architecture architectureEnum = Architecture.fromStringWithFallback(architecture);
        if (Architecture.ARM64.equals(architectureEnum)) {
            filter.put(AwsPlatformResourcesFilterConstants.ARCHITECTURE, architectureEnum.name());
        }
        request.setFilters(filter);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public RegionResponse getRegionsByCredential(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            boolean availabilityZonesNeeded) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/regions, request: {}", request);
        CloudRegions regions = platformParameterService.getRegionsByCredential(request, availabilityZonesNeeded);
        RegionResponse response = platformRegionsToRegionV1ResponseConverter.convert(regions);
        LOGGER.info("Resp /platform_resources/regions, request: {}, regions: {}, response: {}", request, regions, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformNetworksResponse getCloudNetworks(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/networks, request: {}", request);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        PlatformNetworksResponse response = cloudNetworksToPlatformNetworksV1ResponseConverter.convert(networks);
        LOGGER.info("Resp /platform_resources/networks, request: {}, networks: {}, response: {}", request, networks, response);
        return response;

    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformIpPoolsResponse getIpPoolsCredentialId(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/ip_pools, request: {}", request);
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(request);
        PlatformIpPoolsResponse response = cloudIpPoolsToPlatformIpPoolsV1ResponseConverter.convert(ipPools);
        LOGGER.info("Resp /platform_resources/ip_pools, request: {}, ipPools: {}, response: {}", request, ipPools, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformGatewaysResponse getGatewaysCredentialId(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/gateways, request: {}", request);
        CloudGateWays gateways = platformParameterService.getGatewaysCredentialId(request);
        PlatformGatewaysResponse response = cloudGatewayssToPlatformGatewaysV1ResponseConverter.convert(gateways);
        LOGGER.info("Resp /platform_resources/gateways, request: {}, ipPools: {}, response: {}", request, gateways, response);
        return response;

    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformEncryptionKeysResponse getEncryptionKeys(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/encryption_keys, request: {}", request);
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(request);
        PlatformEncryptionKeysResponse response = cloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter.convert(encryptionKeys);
        LOGGER.info("Resp /platform_resources/encryption_keys, request: {}, ipPools: {}, response: {}", request, encryptionKeys, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformSecurityGroupsResponse getSecurityGroups(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/security_groups, request: {}", request);
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        PlatformSecurityGroupsResponse response = cloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter.convert(securityGroups);
        LOGGER.info("Resp /platform_resources/security_groups, request: {}, securityGroups: {}, response: {}", request, securityGroups, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformSshKeysResponse getCloudSshKeys(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/ssh_keys, request: {}", request);
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(request);
        PlatformSshKeysResponse response = cloudSshKeysToPlatformSshKeysV1ResponseConverter.convert(sshKeys);
        LOGGER.info("Resp /platform_resources/ssh_keys, request: {}, sshKeys: {}, response: {}", request, sshKeys, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformAccessConfigsResponse getAccessConfigs(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            AccessConfigTypeQueryParam accessConfigType) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                accessConfigType);
        LOGGER.info("Get /platform_resources/access_configs, request: {}", request);
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(request);
        PlatformAccessConfigsResponse response = cloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter.convert(accessConfigs);
        LOGGER.info("Resp /platform_resources/access_configs, request: {}, accessConfigs: {}, response: {}", request, accessConfigs, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformNoSqlTablesResponse getNoSqlTables(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/nosql_tables, request: {}", request);
        CloudNoSqlTables noSqlTables = platformParameterService.getNoSqlTables(request);
        PlatformNoSqlTablesResponse response = cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter.convert(noSqlTables);
        LOGGER.info("Resp /platform_resources/nosql_tables, request: {}, noSqlTables: {}, response: {}", request, noSqlTables, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformResourceGroupsResponse getResourceGroups(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                null,
                null,
                null);
        LOGGER.info("Get /platform_resources/resource_groups, request: {}", request);
        CloudResourceGroups resourceGroups = platformParameterService.getResourceGroups(request);
        List<PlatformResourceGroupResponse> platformResourceGroups = resourceGroups.getResourceGroups().stream()
                .map(rg -> new PlatformResourceGroupResponse(rg.getName()))
                .collect(Collectors.toList());
        PlatformResourceGroupsResponse response = new PlatformResourceGroupsResponse(platformResourceGroups);
        LOGGER.info("Resp /platform_resources/resource_groups, request: {}, resourceGroups: {}, response: {}", request, resourceGroups, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformPrivateDnsZonesResponse getPrivateDnsZones(
            @ResourceCrn String environmentCrn,
            String platformVariant) {
        String accountId = getAccountId();
        validateEnvironmentCrnPattern(environmentCrn);
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                platformVariant,
                null);
        LOGGER.debug("Get /platform_resources/private_dns_zones, request: {}", request);
        CloudPrivateDnsZones privateDnsZones = platformParameterService.getPrivateDnsZones(request);
        List<PlatformPrivateDnsZoneResponse> platformPrivateDnsZones = privateDnsZones.getPrivateDnsZones().stream()
                .map(pdz -> new PlatformPrivateDnsZoneResponse(pdz.getPrivateDnsZoneId()))
                .collect(Collectors.toList());
        PlatformPrivateDnsZonesResponse response = new PlatformPrivateDnsZonesResponse(platformPrivateDnsZones);
        LOGGER.debug("Resp /platform_resources/private_dns_zones, request: {}, privateDnsZones: {}, response: {}", request, privateDnsZones, response);
        return response;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformDatabaseCapabilitiesResponse getDatabaseCapabilities(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            DatabaseCapabilityType databaseType,
            String architecture) {
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequestByEnvironment(
                accountId,
                environmentCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                databaseType);
        if (architecture != null) {
            request.getFilters().put("architecture", Architecture.fromStringWithValidation(architecture).getName());
        }
        LOGGER.info("Get /platform_resources/database_capabilities, request: {}", request);
        PlatformDatabaseCapabilities platformDatabaseCapabilities = platformParameterService.getDatabaseCapabilities(request);
        PlatformDatabaseCapabilitiesResponse response = databaseCapabilitiesToPlatformDatabaseCapabilitiesResponseConverter
                .convert(platformDatabaseCapabilities);
        LOGGER.info("Resp /platform_resources/database_capabilities, request: {}, response: {}", request, response);
        return response;
    }

    private String getAccountId() {
        return ThreadBasedUserCrnProvider.getAccountId();
    }

    private void validateEnvironmentCrnPattern(String environmentCrn) {
        try {
            Crn.safeFromString(environmentCrn);
        } catch (CrnParseException e) {
            throw new BadRequestException(String.format("The 'environmentCrn' field value is not a valid CRN: '%s'", e));
        }
    }
}
