package com.sequenceiq.cloudbreak.service.platform;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
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
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.CloudResourceAdvisor;

@Service
public class PlatformParameterService {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CloudResourceAdvisor cloudResourceAdvisor;

    @Inject
    private CredentialService credentialService;

    public PlatformResourceRequest getPlatformResourceRequest(Long workspaceId, String credentialName, String region, String platformVariant,
            String availabilityZone) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();

        if (!Strings.isNullOrEmpty(credentialName)) {
            platformResourceRequest.setCredential(credentialService.getByNameForWorkspaceId(credentialName, workspaceId));
        } else {
            throw new BadRequestException("The credentialId or the credentialName must be specified in the request");
        }
        if (!Strings.isNullOrEmpty(platformVariant)) {
            platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().cloudPlatform());
        } else {
            platformResourceRequest.setPlatformVariant(
                    Strings.isNullOrEmpty(platformVariant) ? platformResourceRequest.getCredential().cloudPlatform() : platformVariant);
        }
        platformResourceRequest.setRegion(region);
        platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().cloudPlatform());
        if (!Strings.isNullOrEmpty(availabilityZone)) {
            platformResourceRequest.setAvailabilityZone(availabilityZone);
        }
        return platformResourceRequest;
    }

    public CloudVmTypes getVmTypesByCredential(PlatformResourceRequest request) {
        checkFieldIsNotEmpty(request.getRegion(), "region");
        return cloudParameterService.getVmTypesV2(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudRegions getRegionsByCredential(PlatformResourceRequest request) {
        return cloudParameterService.getRegionsV2(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public PlatformDisks getDiskTypes() {
        return cloudParameterService.getDiskTypes();
    }

    public CloudNetworks getCloudNetworks(PlatformResourceRequest request) {
        return cloudParameterService.getCloudNetworks(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudIpPools getIpPoolsCredentialId(PlatformResourceRequest request) {
        return cloudParameterService.getPublicIpPools(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudGateWays getGatewaysCredentialId(PlatformResourceRequest request) {
        return cloudParameterService.getGateways(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudEncryptionKeys getEncryptionKeys(PlatformResourceRequest request) {
        return cloudParameterService.getCloudEncryptionKeys(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public PlatformRecommendation getRecommendation(Long workspaceId, String clusterDefinitionName, String credentialName,
            String region, String platformVariant, String availabilityZone) {
        if (!ObjectUtils.allNotNull(region, availabilityZone)) {
            throw new BadRequestException("region and availabilityZone cannot be null");
        }
        return cloudResourceAdvisor.createForClusterDefintion(workspaceId, clusterDefinitionName, credentialName, region, platformVariant, availabilityZone);
    }

    public CloudSecurityGroups getSecurityGroups(PlatformResourceRequest request) {
        return cloudParameterService.getSecurityGroups(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudSshKeys getCloudSshKeys(PlatformResourceRequest request) {
        return cloudParameterService.getCloudSshKeys(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    public CloudAccessConfigs getAccessConfigs(PlatformResourceRequest request) {
        return cloudParameterService.getCloudAccessConfigs(request.getCredential(), request.getRegion(),
                request.getPlatformVariant(), request.getFilters());
    }

    private void checkFieldIsNotEmpty(Object field, String param) {
        if (StringUtils.isEmpty(field)) {
            throw new BadRequestException(String.format("The '%s' request body field is mandatory for recommendation creation.", param));
        }
    }

}
