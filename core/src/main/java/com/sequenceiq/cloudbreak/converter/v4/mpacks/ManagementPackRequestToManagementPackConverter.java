package com.sequenceiq.cloudbreak.converter.v4.mpacks;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.request.ManagementPackV4Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class ManagementPackRequestToManagementPackConverter extends AbstractConversionServiceAwareConverter<ManagementPackV4Request, ManagementPack> {
    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public ManagementPack convert(ManagementPackV4Request source) {
        ManagementPack mpack = new ManagementPack();
        if (Strings.isNullOrEmpty(source.getName())) {
            mpack.setName(missingResourceNameGenerator.generateName(APIResourceType.MANAGEMENT_PACK));
        } else {
            mpack.setName(source.getName());
        }
        mpack.setName(source.getName());
        mpack.setDescription(source.getDescription());
        mpack.setMpackUrl(source.getMpackUrl());
        mpack.setPurge(source.isPurge());
        mpack.setIgnoreValidation(source.isIgnoreValidation());
        mpack.setPurgeList(String.join(",", source.getPurgeList()));
        mpack.setForce(source.isForce());
        return mpack;
    }
}
