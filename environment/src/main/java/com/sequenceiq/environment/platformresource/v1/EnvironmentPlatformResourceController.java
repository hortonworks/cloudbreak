package com.sequenceiq.environment.platformresource.v1;

import java.util.List;
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

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.cloudbreak.cloud.model.resourcegroup.CloudResourceGroups;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.AccessConfigTypeQueryParam;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformAccessConfigsResponse;
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
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Controller
@Transactional(TxType.NEVER)
public class EnvironmentPlatformResourceController implements EnvironmentPlatformResourceEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentPlatformResourceController.class);

    @Inject
    @Named("conversionService")
    private ConversionService convertersionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public PlatformVmtypesResponse getVmTypesByCredential(
            @ResourceCrn String environmentCrn,
            String region,
            String platformVariant,
            String availabilityZone,
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
                cdpResourceType);
        LOGGER.info("Get /platform_resources/machine_types, request: {}", request);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        PlatformVmtypesResponse response = convertersionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
        LOGGER.info("Resp /platform_resources/machine_types, request: {}, cloudVmTypes: {}, response: {}", request, cloudVmTypes, response);
        return response;
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
                null);
        LOGGER.info("Get /platform_resources/regions, request: {}", request);
        CloudRegions regions = platformParameterService.getRegionsByCredential(request, availabilityZonesNeeded);
        RegionResponse response = convertersionService.convert(regions, RegionResponse.class);
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
                sharedProjectId);
        LOGGER.info("Get /platform_resources/networks, request: {}", request);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        PlatformNetworksResponse response = convertersionService.convert(networks, PlatformNetworksResponse.class);
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
                null);
        LOGGER.info("Get /platform_resources/ip_pools, request: {}", request);
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(request);
        PlatformIpPoolsResponse response = convertersionService.convert(ipPools, PlatformIpPoolsResponse.class);
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
                null);
        LOGGER.info("Get /platform_resources/gateways, request: {}", request);
        CloudGateWays gateways = platformParameterService.getGatewaysCredentialId(request);
        PlatformGatewaysResponse response = convertersionService.convert(gateways, PlatformGatewaysResponse.class);
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
                null);
        LOGGER.info("Get /platform_resources/encryption_keys, request: {}", request);
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(request);
        PlatformEncryptionKeysResponse response = convertersionService.convert(encryptionKeys, PlatformEncryptionKeysResponse.class);
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
                sharedProjectId);
        LOGGER.info("Get /platform_resources/security_groups, request: {}", request);
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        PlatformSecurityGroupsResponse response = convertersionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
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
                null);
        LOGGER.info("Get /platform_resources/ssh_keys, request: {}", request);
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(request);
        PlatformSshKeysResponse response = convertersionService.convert(sshKeys, PlatformSshKeysResponse.class);
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
        PlatformAccessConfigsResponse response = convertersionService.convert(accessConfigs, PlatformAccessConfigsResponse.class);
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
                null);
        LOGGER.info("Get /platform_resources/nosql_tables, request: {}", request);
        CloudNoSqlTables noSqlTables = platformParameterService.getNoSqlTables(request);
        PlatformNoSqlTablesResponse response = convertersionService.convert(noSqlTables, PlatformNoSqlTablesResponse.class);
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
