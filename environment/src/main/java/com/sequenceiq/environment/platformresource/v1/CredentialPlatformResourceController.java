package com.sequenceiq.environment.platformresource.v1;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.dns.CloudPrivateDnsZones;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.platformresource.CredentialPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDisksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformPrivateDnsZoneResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformPrivateDnsZonesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformRequirementsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.TagSpecificationsResponse;
import com.sequenceiq.environment.environment.service.EnvironmentRequirementService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.environment.platformresource.v1.converter.CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudDatabaseVmTypesToPlatformDatabaseVmTypesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudGatewayssToPlatformGatewaysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudIpPoolsToPlatformIpPoolsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudNetworksToPlatformNetworksV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudSshKeysToPlatformSshKeysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudVmTypesToPlatformVmTypesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformDisksToPlatformDisksV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformRegionsToRegionV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.TagSpecificationsToTagSpecificationsV1ResponseConverter;

@Controller
@Transactional(TxType.NEVER)
public class CredentialPlatformResourceController implements CredentialPlatformResourceEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialPlatformResourceController.class);

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private EnvironmentRequirementService environmentRequirementService;

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private CloudVmTypesToPlatformVmTypesV1ResponseConverter cloudVmTypesToPlatformVmTypesV1ResponseConverter;

    @Inject
    private PlatformRegionsToRegionV1ResponseConverter platformRegionsToRegionV1ResponseConverter;

    @Inject
    private PlatformDisksToPlatformDisksV1ResponseConverter platformDisksToPlatformDisksV1ResponseConverter;

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
    private CloudDatabaseVmTypesToPlatformDatabaseVmTypesV1ResponseConverter cloudDatabaseVmTypesToPlatformDatabaseVmTypesV1ResponseConverter;

    @Inject
    private CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter cloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter;

    @Inject
    private TagSpecificationsToTagSpecificationsV1ResponseConverter tagSpecificationsToTagSpecificationsV1ResponseConverter;

    @Inject
    private CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter;

    @Inject
    private CustomCheckUtil customCheckUtil;

    @Override
    @CustomPermissionCheck
    public PlatformVmtypesResponse getVmTypesByCredential(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            CdpResourceType cdpResourceType,
            String architecture) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                Map.of(ARCHITECTURE, Optional.ofNullable(architecture).orElse(Architecture.X86_64.getName())),
                null,
                null,
                cdpResourceType);
        LOGGER.info("Get /platform_resources/machine_types, request: {}", request);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        PlatformVmtypesResponse response = cloudVmTypesToPlatformVmTypesV1ResponseConverter.convert(cloudVmTypes);
        LOGGER.info("Resp /platform_resources/machine_types, request: {}, cloudVmTypes: {}, response: {}", request, cloudVmTypes, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public RegionResponse getRegionsByCredential(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            boolean availabilityZonesNeeded) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/regions, request: {}", request);
        CloudRegions regions = platformParameterService.getRegionsByCredential(request, availabilityZonesNeeded);
        RegionResponse response = platformRegionsToRegionV1ResponseConverter.convert(regions);
        LOGGER.info("Resp /platform_resources/regions, request: {}, regions: {}, response: {}", request, regions, response);
        return response;
    }

    @Override
    @DisableCheckPermissions
    public PlatformDisksResponse getDisktypes() {
        LOGGER.info("Get /platform_resources/disk_types");
        PlatformDisks disks = platformParameterService.getDiskTypes();
        PlatformDisksResponse response = platformDisksToPlatformDisksV1ResponseConverter.convert(disks);
        LOGGER.info("Resp /platform_resources/disk_types, disks: {}, response: {}", disks, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformNetworksResponse getCloudNetworks(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String networkId,
            String subnetIds,
            String sharedProjectId) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        Map<String, String> filter = new HashMap<>();
        if (!Strings.isNullOrEmpty(subnetIds)) {
            filter.put(NetworkConstants.SUBNET_IDS, subnetIds);
        }
        if (!Strings.isNullOrEmpty(networkId)) {
            filter.put("networkId", networkId);
        }
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId,
                filter,
                null,
                null,
                CdpResourceType.DEFAULT);
        LOGGER.info("Get /platform_resources/networks, request: {}", request);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        PlatformNetworksResponse response = cloudNetworksToPlatformNetworksV1ResponseConverter.convert(networks);
        LOGGER.info("Resp /platform_resources/networks, request: {}, networks: {}, response: {}", request, networks, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformIpPoolsResponse getIpPoolsCredentialId(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/ip_pools, request: {}", request);
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(request);
        PlatformIpPoolsResponse response = cloudIpPoolsToPlatformIpPoolsV1ResponseConverter.convert(ipPools);
        LOGGER.info("Resp /platform_resources/ip_pools, request: {}, ipPools: {}, response: {}", request, ipPools, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformGatewaysResponse getGatewaysCredentialId(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/gateways, request: {}", request);
        CloudGateWays gateways = platformParameterService.getGatewaysCredentialId(request);
        PlatformGatewaysResponse response = cloudGatewayssToPlatformGatewaysV1ResponseConverter.convert(gateways);
        LOGGER.info("Resp /platform_resources/gateways, request: {}, ipPools: {}, response: {}", request, gateways, response);
        return response;

    }

    @Override
    @CustomPermissionCheck
    public PlatformEncryptionKeysResponse getEncryptionKeys(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/encryption_keys, request: {}", request);
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(request);
        PlatformEncryptionKeysResponse response = cloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter.convert(encryptionKeys);
        LOGGER.info("Resp /platform_resources/encryption_keys, request: {}, ipPools: {}, response: {}", request, encryptionKeys, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformSecurityGroupsResponse getSecurityGroups(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            String sharedProjectId) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                sharedProjectId);
        LOGGER.info("Get /platform_resources/security_groups, request: {}", request);
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        PlatformSecurityGroupsResponse response = cloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter.convert(securityGroups);
        LOGGER.info("Resp /platform_resources/security_groups, request: {}, securityGroups: {}, response: {}", request, securityGroups, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformSshKeysResponse getCloudSshKeys(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/ssh_keys, request: {}", request);
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(request);
        PlatformSshKeysResponse response = cloudSshKeysToPlatformSshKeysV1ResponseConverter.convert(sshKeys);
        LOGGER.info("Resp /platform_resources/ssh_keys, request: {}, sshKeys: {}", request, sshKeys);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformAccessConfigsResponse getAccessConfigs(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            AccessConfigTypeQueryParam accessConfigType) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
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
    @DisableCheckPermissions
    public TagSpecificationsResponse getTagSpecifications() {
        LOGGER.info("Get /platform_resources/tag_specifications");
        Map<Platform, PlatformParameters> platformParameters = platformParameterService.getPlatformParameters();
        TagSpecificationsResponse response = tagSpecificationsToTagSpecificationsV1ResponseConverter.convert(platformParameters);
        LOGGER.info("Resp /platform_resources/tag_specifications, platformParameters: {}, response: {}", platformParameters, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformNoSqlTablesResponse getNoSqlTables(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/nosql_tables, request: {}", request);
        CloudNoSqlTables noSqlTables = platformParameterService.getNoSqlTables(request);
        PlatformNoSqlTablesResponse response = cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter.convert(noSqlTables);
        LOGGER.info("Resp /platform_resources/nosql_tables, request: {}, noSqlTables: {}, response: {}", request, noSqlTables, response);
        return response;
    }

    @Override
    @CustomPermissionCheck
    public PlatformResourceGroupsResponse getResourceGroups(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                CdpResourceType.DEFAULT);
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
    @CustomPermissionCheck
    public PlatformPrivateDnsZonesResponse getPrivateDnsZones(
            String credentialName,
            String credentialCrn,
            String platformVariant) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                platformVariant,
                CdpResourceType.DEFAULT);
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
    @CustomPermissionCheck
    // This endpoint used only for validating new regions. NOT for customer usage!
    public PlatformRequirementsResponse getRequirements(String credentialName, String credentialCrn, String region) {
        customCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        PlatformResourceRequest requirements = platformParameterService.getRequirements(getAccountId(), credentialName, credentialCrn, region);
        return environmentRequirementService.getPlatformRequirementsResponse(requirements);
    }

    private String getAccountId() {
        return ThreadBasedUserCrnProvider.getAccountId();
    }

    private void permissionCheckByCredential(String credentialName, String credentialCrn) {
        if (!Strings.isNullOrEmpty(credentialName)) {
            commonPermissionCheckingUtils.checkPermissionForUserOnResource(AuthorizationResourceAction.DESCRIBE_CREDENTIAL,
                    ThreadBasedUserCrnProvider.getUserCrn(), platformParameterService.getCredentialCrnByName(credentialName));
        } else if (!Strings.isNullOrEmpty(credentialCrn)) {
            validateCredentialCrnPattern(credentialCrn);
            commonPermissionCheckingUtils.checkPermissionForUserOnResource(AuthorizationResourceAction.DESCRIBE_CREDENTIAL,
                    ThreadBasedUserCrnProvider.getUserCrn(), credentialCrn);
        } else {
            throw new BadRequestException("The credentialCrn or the credentialName must be specified in the request");
        }
    }

    private void validateCredentialCrnPattern(String credentialCrn) {
        try {
            Crn.safeFromString(credentialCrn);
        } catch (CrnParseException e) {
            throw new BadRequestException(String.format("The 'credentialCrn' field value is not a valid CRN: '%s'", e));
        }
    }
}
