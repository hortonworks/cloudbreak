package com.sequenceiq.cloudbreak.service.platform;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.RecommendationV4Request;
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
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.stack.CloudResourceAdvisor;
import com.sequenceiq.cloudbreak.service.user.UserService;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;

@Service
public class PlatformParameterService {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private CloudResourceAdvisor cloudResourceAdvisor;

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

    public PlatformRecommendation getRecommendation(Long workspaceId, RecommendationV4Request request) {
        PlatformResourceRequest resourceRequest = conversionService.convert(request, PlatformResourceRequest.class);
        if (request.getBlueprintId() == null && Strings.isNullOrEmpty(request.getBlueprintName())) {
            checkFieldIsNotEmpty(request.getBlueprintId(), "blueprintId");
        }
        checkFieldIsNotEmpty(request.getRegion(), "region");
        checkFieldIsNotEmpty(request.getAvailabilityZone(), "availabilityZone");
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(workspaceId, user);
        return cloudResourceAdvisor.createForBlueprint(request.getBlueprintName(), request.getBlueprintId(),
                        resourceRequest, user, workspace);
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
