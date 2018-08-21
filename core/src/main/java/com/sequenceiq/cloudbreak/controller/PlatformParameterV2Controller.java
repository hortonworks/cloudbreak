package com.sequenceiq.cloudbreak.controller;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v2.ConnectorV2Endpoint;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformVmtypesResponse;
import com.sequenceiq.cloudbreak.api.model.RegionResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
@Transactional(TxType.NEVER)
public class PlatformParameterV2Controller implements ConnectorV2Endpoint {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public RegionResponse getRegionsByCredentialId(PlatformResourceRequestJson resourceRequestJson) {
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);

        CloudRegions cloudRegions = cloudParameterService.getRegionsV2(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudRegions, RegionResponse.class);
    }

    @Override
    public PlatformVmtypesResponse getVmTypesByCredentialId(PlatformResourceRequestJson resourceRequestJson) {
        PlatformResourceRequest convert = conversionService.convert(resourceRequestJson, PlatformResourceRequest.class);
        fieldIsNotEmpty(resourceRequestJson.getRegion(), "region");
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(convert.getCredential(), convert.getRegion(),
                convert.getPlatformVariant(), convert.getFilters());
        return conversionService.convert(cloudVmTypes, PlatformVmtypesResponse.class);
    }

    private void fieldIsNotEmpty(Object field, String fieldName) {
        if (StringUtils.isEmpty(field)) {
            throw new BadRequestException(String.format("The '%s' request body field is mandatory for recommendation creation.", fieldName));
        }
    }
}
