package com.sequenceiq.cloudbreak.controller;

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
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

@Component
@Transactional(TxType.NEVER)
public class PlatformParameterV3Controller implements ConnectorV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Override
    public PlatformVmtypesResponse getVmTypesByCredential(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudVmTypes cloudVmTypes = platformParameterService.getVmTypesByCredential(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
    }

    @Override
    public RegionResponse getRegionsByCredential(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudRegions regions = platformParameterService.getRegionsByCredential(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(regions, RegionResponse.class);
    }

    @Override
    public PlatformDisksJson getDisktypes(Long organizationId) {
        PlatformDisks disks = platformParameterService.getDiskTypes();
        return conversionService.convert(disks, PlatformDisksJson.class);
    }

    @Override
    public PlatformNetworksResponse getCloudNetworks(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudNetworks networks = platformParameterService.getCloudNetworks(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(networks, PlatformNetworksResponse.class);
    }

    @Override
    public PlatformIpPoolsResponse getIpPoolsCredentialId(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudIpPools ipPools = platformParameterService.getIpPoolsCredentialId(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(ipPools, PlatformIpPoolsResponse.class);
    }

    @Override
    public PlatformGatewaysResponse getGatewaysCredentialId(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudGateWays gateWays = platformParameterService.getGatewaysCredentialId(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(gateWays, PlatformGatewaysResponse.class);
    }

    @Override
    public PlatformEncryptionKeysResponse getEncryptionKeys(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudEncryptionKeys encryptionKeys = platformParameterService.getEncryptionKeys(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(encryptionKeys, PlatformEncryptionKeysResponse.class);
    }

    @Override
    public RecommendationResponse createRecommendation(Long organizationId, RecommendationRequestJson recommendationRequestJson) {
        return conversionService.convert(platformParameterService.getRecommendation(organizationId, recommendationRequestJson), RecommendationResponse.class);
    }

    @Override
    public PlatformSecurityGroupsResponse getSecurityGroups(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(securityGroups, PlatformSecurityGroupsResponse.class);
    }

    @Override
    public PlatformSshKeysResponse getCloudSshKeys(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudSshKeys sshKeys = platformParameterService.getCloudSshKeys(conversionService.convert(resourceRequestJson, PlatformResourceRequest.class));
        return conversionService.convert(sshKeys, PlatformSshKeysResponse.class);
    }

    @Override
    public PlatformAccessConfigsResponse getAccessConfigs(Long organizationId, PlatformResourceRequestJson resourceRequestJson) {
        CloudAccessConfigs accessConfigs = platformParameterService.getAccessConfigs(conversionService.convert(resourceRequestJson,
                PlatformResourceRequest.class));
        return conversionService.convert(accessConfigs, PlatformAccessConfigsResponse.class);
    }
}
