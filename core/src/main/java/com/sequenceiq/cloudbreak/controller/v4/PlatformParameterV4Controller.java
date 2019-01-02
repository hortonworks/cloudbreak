package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.ConnectorV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformGatewaysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformIpPoolsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.RegionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.TagSpecificationsV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
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
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Controller
@Transactional(TxType.NEVER)
public class PlatformParameterV4Controller implements ConnectorV4Endpoint {

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    public PlatformVmtypesV4Response getVmTypesByCredential(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(request);
        return converterUtil.convert(cloudVmTypes, PlatformVmtypesV4Response.class);
    }

    @Override
    public RegionV4Response getRegionsByCredential(Long workspaceId, String credentialName, String region, String platformVariant, String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudRegions regions = platformParameterService.getRegionsByCredential(request);
        return converterUtil.convert(regions, RegionV4Response.class);
    }

    @Override
    public PlatformDisksV4Response getDisktypes(Long workspaceId) {
        PlatformDisks disks = platformParameterService.getDiskTypes();
        return converterUtil.convert(disks, PlatformDisksV4Response.class);
    }

    @Override
    public PlatformNetworksV4Response getCloudNetworks(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudNetworks networks = platformParameterService.getCloudNetworks(request);
        return converterUtil.convert(networks, PlatformNetworksV4Response.class);
    }

    @Override
    public PlatformIpPoolsV4Response getIpPoolsCredentialId(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(request);
        return converterUtil.convert(ipPools, PlatformIpPoolsV4Response.class);
    }

    @Override
    public PlatformGatewaysV4Response getGatewaysCredentialId(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudGateWays gateWays = platformParameterService.getGatewaysCredentialId(request);
        return converterUtil.convert(gateWays, PlatformGatewaysV4Response.class);
    }

    @Override
    public PlatformEncryptionKeysV4Response getEncryptionKeys(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(request);
        return converterUtil.convert(encryptionKeys, PlatformEncryptionKeysV4Response.class);
    }

    @Override
    public PlatformSecurityGroupsV4Response getSecurityGroups(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);
        return converterUtil.convert(securityGroups, PlatformSecurityGroupsV4Response.class);
    }

    @Override
    public PlatformSshKeysV4Response getCloudSshKeys(Long workspaceId, String credentialName, String region, String platformVariant, String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(request);
        return converterUtil.convert(sshKeys, PlatformSshKeysV4Response.class);
    }

    @Override
    public PlatformAccessConfigsV4Response getAccessConfigs(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(workspaceId, credentialName, region, platformVariant,
                availabilityZone);
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(request);
        return converterUtil.convert(accessConfigs, PlatformAccessConfigsV4Response.class);
    }

    @Override
    public TagSpecificationsV4Response getTagSpecifications(Long workspaceId) {
        Map<Platform, PlatformParameters> platformParameters = cloudParameterService.getPlatformParameters();
        return converterUtil.convert(platformParameters, TagSpecificationsV4Response.class);
    }
}
