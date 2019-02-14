package com.sequenceiq.cloudbreak.converter.v4.mpacks;

import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.response.ManagementPackV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ManagementPack;

@Component
public class ManagementPackToManagementPackResponseConverter extends AbstractConversionServiceAwareConverter<ManagementPack, ManagementPackV4Response> {
    @Override
    public ManagementPackV4Response convert(ManagementPack source) {
        ManagementPackV4Response mpackResponse = new ManagementPackV4Response();
        mpackResponse.setId(source.getId());
        mpackResponse.setName(source.getName());
        mpackResponse.setDescription(source.getDescription());
        mpackResponse.setMpackUrl(source.getMpackUrl());
        mpackResponse.setPurge(source.isPurge());
        mpackResponse.setIgnoreValidation(source.isIgnoreValidation());
        if (StringUtils.hasLength(source.getPurgeList())) {
            mpackResponse.setPurgeList(Arrays.asList(source.getPurgeList().split(",")));
        }
        mpackResponse.setForce(source.isForce());
        return mpackResponse;
    }
}
