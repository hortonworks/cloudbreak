package com.sequenceiq.environment.platformresource.v1;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
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
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceEndpoint;
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
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Controller
@Transactional(TxType.NEVER)
public class PlatformResourceController implements PlatformResourceEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformResourceController.class);

    @Inject
    @Named("conversionService")
    private ConversionService convertersionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    public PlatformVmtypesResponse getVmTypesByCredential(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            CdpResourceType cdpResourceType) {
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                null,
                cdpResourceType);
        LOGGER.info("Get /platform_resources/machine_types, request: {}", request);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        PlatformVmtypesResponse response = convertersionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
        LOGGER.info("Resp /platform_resources/machine_types, request: {}, cloudVmTypes: {}, response: {}", request, cloudVmTypes, response);
        return response;
    }

    @Override
    public RegionResponse getRegionsByCredential(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/regions, request: {}", request);
        CloudRegions regions = platformParameterService.getRegionsByCredential(request);
        RegionResponse response = convertersionService.convert(regions, RegionResponse.class);
        LOGGER.info("Resp /platform_resources/regions, request: {}, regions: {}, response: {}", request, regions, response);
        return response;
    }

    @Override
    public PlatformDisksResponse getDisktypes() {
        LOGGER.info("Get /platform_resources/disk_types");
        PlatformDisks disks = platformParameterService.getDiskTypes();
        PlatformDisksResponse response = convertersionService.convert(disks, PlatformDisksResponse.class);
        LOGGER.info("Resp /platform_resources/disk_types, disks: {}, response: {}", disks, response);
        return response;
    }

    @Override
    public PlatformNetworksResponse getCloudNetworks(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/networks, request: {}", request);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        PlatformNetworksResponse response = convertersionService.convert(networks, PlatformNetworksResponse.class);
        LOGGER.info("Resp /platform_resources/networks, request: {}, networks: {}, response: {}", request, networks, response);
        return response;

    }

    @Override
    public PlatformIpPoolsResponse getIpPoolsCredentialId(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
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
    public PlatformGatewaysResponse getGatewaysCredentialId(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
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
    public PlatformEncryptionKeysResponse getEncryptionKeys(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
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
    public PlatformSecurityGroupsResponse getSecurityGroups(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone);
        LOGGER.info("Get /platform_resources/security_groups, request: {}", request);
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        PlatformSecurityGroupsResponse response = convertersionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
        LOGGER.info("Resp /platform_resources/security_groups, request: {}, securityGroups: {}, response: {}", request, securityGroups, response);
        return response;
    }

    @Override
    public PlatformSshKeysResponse getCloudSshKeys(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
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
    public PlatformAccessConfigsResponse getAccessConfigs(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone,
            AccessConfigTypeQueryParam accessConfigType) {
        String accountId = getAccountId();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                accountId,
                credentialName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                accessConfigType);
        LOGGER.info("Get /platform_resources/access_configs, request: {}", request);
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(request);
        PlatformAccessConfigsResponse response = convertersionService.convert(accessConfigs, PlatformAccessConfigsResponse.class);
        LOGGER.info("Resp /platform_resources/access_configs, request: {}, accessConfigs: {}, response: {}", request, accessConfigs, response);
        return response;
    }

    @Override
    public TagSpecificationsResponse getTagSpecifications() {
        LOGGER.info("Get /platform_resources/tag_specifications");
        Map<Platform, PlatformParameters> platformParameters = platformParameterService.getPlatformParameters();
        TagSpecificationsResponse response = convertersionService.convert(platformParameters, TagSpecificationsResponse.class);
        LOGGER.info("Resp /platform_resources/tag_specifications, platformParameters: {}, response: {}", platformParameters, response);
        return response;
    }

    @Override
    public PlatformNoSqlTablesResponse getNoSqlTables(
            String credentialName,
            String credentialCrn,
            String region,
            String platformVariant,
            String availabilityZone) {
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

    private String getAccountId() {
        return ThreadBasedUserCrnProvider.getAccountId();
    }
}
