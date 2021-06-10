package com.sequenceiq.environment.platformresource.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
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
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.platformresource.CredentialPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDisksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformGatewaysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformIpPoolsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNetworksResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformResourceGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformSshKeysResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.RegionResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.TagSpecificationsResponse;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Controller
@Transactional(TxType.NEVER)
public class CredentialPlatformResourceController implements CredentialPlatformResourceEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialPlatformResourceController.class);

    @Inject
    @Named("conversionService")
    private ConversionService convertersionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Override
    @CustomPermissionCheck
    public PlatformVmtypesResponse getVmTypesByCredential(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            CdpResourceType cdpResourceType) {
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                new HashMap<>(),
                null,
                cdpResourceType);
        LOGGER.info("Get /platform_resources/machine_types, request: {}", request);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        PlatformVmtypesResponse response = convertersionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        RegionResponse response = convertersionService.convert(regions, RegionResponse.class);
        LOGGER.info("Resp /platform_resources/regions, request: {}, regions: {}, response: {}", request, regions, response);
        return response;
    }

    @Override
    @DisableCheckPermissions
    public PlatformDisksResponse getDisktypes() {
        LOGGER.info("Get /platform_resources/disk_types");
        PlatformDisks disks = platformParameterService.getDiskTypes();
        PlatformDisksResponse response = convertersionService.convert(disks, PlatformDisksResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
                CdpResourceType.DEFAULT);
        LOGGER.info("Get /platform_resources/networks, request: {}", request);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        PlatformNetworksResponse response = convertersionService.convert(networks, PlatformNetworksResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformIpPoolsResponse response = convertersionService.convert(ipPools, PlatformIpPoolsResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformGatewaysResponse response = convertersionService.convert(gateways, PlatformGatewaysResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformEncryptionKeysResponse response = convertersionService.convert(encryptionKeys, PlatformEncryptionKeysResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformSecurityGroupsResponse response = convertersionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformSshKeysResponse response = convertersionService.convert(sshKeys, PlatformSshKeysResponse.class);
        LOGGER.info("Resp /platform_resources/ssh_keys, request: {}, sshKeys: {}, response: {}", request, sshKeys, response);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformAccessConfigsResponse response = convertersionService.convert(accessConfigs, PlatformAccessConfigsResponse.class);
        LOGGER.info("Resp /platform_resources/access_configs, request: {}, accessConfigs: {}, response: {}", request, accessConfigs, response);
        return response;
    }

    @Override
    @DisableCheckPermissions
    public TagSpecificationsResponse getTagSpecifications() {
        LOGGER.info("Get /platform_resources/tag_specifications");
        Map<Platform, PlatformParameters> platformParameters = platformParameterService.getPlatformParameters();
        TagSpecificationsResponse response = convertersionService.convert(platformParameters, TagSpecificationsResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
        PlatformNoSqlTablesResponse response = convertersionService.convert(noSqlTables, PlatformNoSqlTablesResponse.class);
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
        CustomCheckUtil.run(() -> permissionCheckByCredential(credentialName, credentialCrn));
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
