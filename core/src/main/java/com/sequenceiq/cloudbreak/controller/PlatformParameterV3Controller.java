package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ConnectorV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.PlatformAccessConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.api.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformGatewaysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformIpPoolsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformNetworksResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSecurityGroupsResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformVmtypesResponse;
import com.sequenceiq.cloudbreak.api.model.RecommendationRequestJson;
import com.sequenceiq.cloudbreak.api.model.RecommendationResponse;
import com.sequenceiq.cloudbreak.api.model.RegionResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudAccessConfigs;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudGateWays;
import com.sequenceiq.cloudbreak.cloud.model.CloudIpPools;
import com.sequenceiq.cloudbreak.cloud.model.CloudNetworks;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;

@Controller
@Transactional(TxType.NEVER)
public class PlatformParameterV3Controller implements ConnectorV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    public PlatformVmtypesResponse getVmTypesByCredential(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
    }

    @Override
    public RegionResponse getRegionsByCredential(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudRegions regions = platformParameterService.getRegionsByCredential(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(regions, RegionResponse.class);
    }

    @Override
    public PlatformDisksJson getDisktypes(Long workspaceId) {
        PlatformDisks disks = platformParameterService.getDiskTypes();
        return conversionService.convert(disks, PlatformDisksJson.class);
    }

    @Override
    public PlatformNetworksResponse getCloudNetworks(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudNetworks networks = platformParameterService.getCloudNetworks(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(networks, PlatformNetworksResponse.class);
    }

    @Override
    public PlatformIpPoolsResponse getIpPoolsCredentialId(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(ipPools, PlatformIpPoolsResponse.class);
    }

    @Override
    public PlatformGatewaysResponse getGatewaysCredentialId(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudGateWays gateWays = platformParameterService.getGatewaysCredentialId(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(gateWays, PlatformGatewaysResponse.class);
    }

    @Override
    public PlatformEncryptionKeysResponse getEncryptionKeys(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(encryptionKeys, PlatformEncryptionKeysResponse.class);
    }

    @Override
    public RecommendationResponse createRecommendation(Long workspaceId, RecommendationRequestJson recommendationRequestJson) {
        return conversionService.convert(platformParameterService.getRecommendation(workspaceId, recommendationRequestJson), RecommendationResponse.class);
    }

    @Override
    public PlatformSecurityGroupsResponse getSecurityGroups(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
    }

    @Override
    public PlatformSshKeysResponse getCloudSshKeys(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(conversionService.convert(resourceRequestJson, PlatformResourceRequest.class));
        return conversionService.convert(sshKeys, PlatformSshKeysResponse.class);
    }

    @Override
    public PlatformAccessConfigsResponse getAccessConfigs(Long workspaceId, PlatformResourceRequestJson resourceRequestJson) {
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(accessConfigs, PlatformAccessConfigsResponse.class);
    }
}
