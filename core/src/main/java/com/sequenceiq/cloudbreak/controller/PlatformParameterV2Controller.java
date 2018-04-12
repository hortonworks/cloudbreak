package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v2.ConnectorV2Endpoint;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVmtypesResponse;
import com.sequenceiq.cloudbreak.api.model.RegionResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class PlatformParameterV2Controller implements ConnectorV2Endpoint {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public RegionResponse getRegionsByCredentialId(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);

        CloudRegions cloudRegions = cloudParameterService.getRegionsV2(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudRegions, RegionResponse.class);
    }

    @Override
    public PlatformVmtypesResponse getVmTypesByCredentialId(PlatformResourceRequestJson resourceRequestJson) {
        resourceRequestJson = prepareAccountAndOwner(resourceRequestJson, authenticatedUserService.getCbUser());
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        fieldIsNotEmpty(resourceRequestJson.getRegion(), "region");
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
    }

    private PlatformResourceRequestJson prepareAccountAndOwner(PlatformResourceRequestJson resourceRequestJson, IdentityUser user) {
        resourceRequestJson.setAccount(user.getAccount());
        resourceRequestJson.setOwner(user.getUserId());
        return resourceRequestJson;
    }

    private void fieldIsNotEmpty(Object field, String fieldName) {
        if (StringUtils.isEmpty(field)) {
            throw new BadRequestException(String.format("The '%s' request body field is mandatory for recommendation creation.", fieldName));
        }
    }
}
