package com.sequenceiq.environment.platformresource.v1;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

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
                cdpResourceType);

        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        return convertersionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
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
        CloudRegions regions = platformParameterService.getRegionsByCredential(request);
        return convertersionService.convert(regions, RegionResponse.class);
    }

    @Override
    public PlatformDisksResponse getDisktypes() {
        PlatformDisks disks = platformParameterService.getDiskTypes();
        return convertersionService.convert(disks, PlatformDisksResponse.class);
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
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        return convertersionService.convert(networks, PlatformNetworksResponse.class);
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
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(request);
        return convertersionService.convert(ipPools, PlatformIpPoolsResponse.class);
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
        CloudGateWays gateWays = platformParameterService.getGatewaysCredentialId(request);
        return convertersionService.convert(gateWays, PlatformGatewaysResponse.class);
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
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(request);
        return convertersionService.convert(encryptionKeys, PlatformEncryptionKeysResponse.class);
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
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        return convertersionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
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
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(request);
        return convertersionService.convert(sshKeys, PlatformSshKeysResponse.class);
    }

    @Override
    public PlatformAccessConfigsResponse getAccessConfigs(
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
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(request);
        return convertersionService.convert(accessConfigs, PlatformAccessConfigsResponse.class);
    }

    @Override
    public TagSpecificationsResponse getTagSpecifications() {
        Map<Platform, PlatformParameters> platformParameters = platformParameterService.getPlatformParameters();
        return convertersionService.convert(platformParameters, TagSpecificationsResponse.class);
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
        CloudNoSqlTables noSqlTables = platformParameterService.getNoSqlTables(request);
        return convertersionService.convert(noSqlTables, PlatformNoSqlTablesResponse.class);
    }

    private String getAccountId() {
        return ThreadBasedUserCrnProvider.getAccountId();
    }
}
