package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.domain.ManagementPack;

@Component
public class ManagementPackToManagementPackResponseConverter extends AbstractConversionServiceAwareConverter<ManagementPack, ManagementPackResponse> {
    @Override
    public ManagementPackResponse convert(ManagementPack source) {
        ManagementPackResponse mpackResponse = new ManagementPackResponse();
        mpackResponse.setId(source.getId());
        mpackResponse.setName(source.getName());
        mpackResponse.setDescription(source.getDescription());
        mpackResponse.setMpackUrl(source.getMpackUrl());
        mpackResponse.setPurge(source.isPurge());
        if (StringUtils.hasLength(source.getPurgeList())) {
            mpackResponse.setPurgeList(Arrays.asList(source.getPurgeList().split(",")));
        }
        mpackResponse.setForce(source.isForce());
        return mpackResponse;
    }
}
