package com.sequenceiq.environment.platformresource;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

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
import com.sequenceiq.environment.api.platformresource.PlatformResourceV1Endpoint;
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

@Controller
@Transactional(TxType.NEVER)
public class PlatformParameterV1Controller implements PlatformResourceV1Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService convertersionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    public PlatformVmtypesV1Response getVmTypesByCredential(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        return convertersionService.convert(cloudVmTypes, PlatformVmtypesV1Response.class);
    }

    @Override
    public RegionV1Response getRegionsByCredential(Long workspaceId, String credentialName, String region, String platformVariant, String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudRegions regions = platformParameterService.getRegionsByCredential(request);
        return convertersionService.convert(regions, RegionV1Response.class);
    }

    @Override
    public PlatformDisksV1Response getDisktypes(Long workspaceId) {
        PlatformDisks disks = platformParameterService.getDiskTypes();
        return convertersionService.convert(disks, PlatformDisksV1Response.class);
    }

    @Override
    public PlatformNetworksV1Response getCloudNetworks(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        return convertersionService.convert(networks, PlatformNetworksV1Response.class);
    }

    @Override
    public PlatformIpPoolsV1Response getIpPoolsCredentialId(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(request);
        return convertersionService.convert(ipPools, PlatformIpPoolsV1Response.class);
    }

    @Override
    public PlatformGatewaysV1Response getGatewaysCredentialId(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudGateWays gateWays = platformParameterService.getGatewaysCredentialId(request);
        return convertersionService.convert(gateWays, PlatformGatewaysV1Response.class);
    }

    @Override
    public PlatformEncryptionKeysV1Response getEncryptionKeys(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(request);
        return convertersionService.convert(encryptionKeys, PlatformEncryptionKeysV1Response.class);
    }

    @Override
    public PlatformSecurityGroupsV1Response getSecurityGroups(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        return convertersionService.convert(securityGroups, PlatformSecurityGroupsV1Response.class);
    }

    @Override
    public PlatformSshKeysV1Response getCloudSshKeys(Long workspaceId, String credentialName, String region, String platformVariant, String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(request);
        return convertersionService.convert(sshKeys, PlatformSshKeysV1Response.class);
    }

    @Override
    public PlatformAccessConfigsV1Response getAccessConfigs(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(request);
        return convertersionService.convert(accessConfigs, PlatformAccessConfigsV1Response.class);
    }

    @Override
    public TagSpecificationsV1Response getTagSpecifications(Long workspaceId) {
        Map<Platform, PlatformParameters> platformParameters = platformParameterService.getPlatformParameters();
        return convertersionService.convert(platformParameters, TagSpecificationsV1Response.class);
    }
}
