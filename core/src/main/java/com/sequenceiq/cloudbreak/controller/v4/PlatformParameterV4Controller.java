package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.ConnectorV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.filters.PlatformResourceV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformAccessConfigsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformEncryptionKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformGatewaysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformIpPoolsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformNetworksV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSecurityGroupsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.TagSpecificationsV4Response;
import com.sequenceiq.cloudbreak.api.model.RegionV4Response;
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
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    public PlatformVmtypesV4Response getVmTypesByCredential(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(cloudVmTypes, PlatformVmtypesV4Response.class);
    }

    @Override
    public RegionV4Response getRegionsByCredential(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudRegions regions = platformParameterService.getRegionsByCredential(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(regions, RegionV4Response.class);
    }

    @Override
    public PlatformDisksV4Response getDisktypes(Long workspaceId) {
        PlatformDisks disks = platformParameterService.getDiskTypes();
        return conversionService.convert(disks, PlatformDisksV4Response.class);
    }

    @Override
    public PlatformNetworksV4Response getCloudNetworks(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudNetworks networks = platformParameterService.getCloudNetworks(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(networks, PlatformNetworksV4Response.class);
    }

    @Override
    public PlatformIpPoolsV4Response getIpPoolsCredentialId(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(ipPools, PlatformIpPoolsV4Response.class);
    }

    @Override
    public PlatformGatewaysV4Response getGatewaysCredentialId(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudGateWays gateWays = platformParameterService.getGatewaysCredentialId(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(gateWays, PlatformGatewaysV4Response.class);
    }

    @Override
    public PlatformEncryptionKeysV4Response getEncryptionKeys(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(encryptionKeys, PlatformEncryptionKeysV4Response.class);
    }

    @Override
    public PlatformSecurityGroupsV4Response getSecurityGroups(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(securityGroups, PlatformSecurityGroupsV4Response.class);
    }

    @Override
    public PlatformSshKeysV4Response getCloudSshKeys(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(conversionService.convert(resourceRequestJson, PlatformResourceRequest.class));
        return conversionService.convert(sshKeys, PlatformSshKeysV4Response.class);
    }

    @Override
    public PlatformAccessConfigsV4Response getAccessConfigs(Long workspaceId, PlatformResourceV4Filter resourceRequestJson) {
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(accessConfigs, PlatformAccessConfigsV4Response.class);
    }

    @Override
    public TagSpecificationsV4Response getTagSpecifications(Long workspaceId) {
        Map<Platform, PlatformParameters> platformParameters = cloudParameterService.getPlatformParameters();
        return conversionService.convert(platformParameters, TagSpecificationsV4Response.class);
    }
}
