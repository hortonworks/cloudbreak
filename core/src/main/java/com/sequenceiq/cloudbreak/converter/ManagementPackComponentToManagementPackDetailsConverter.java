package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Component
public class ManagementPackComponentToManagementPackDetailsConverter extends
        AbstractConversionServiceAwareConverter<ManagementPackComponent, ManagementPackDetails> {
    @Override
    public ManagementPackDetails convert(ManagementPackComponent source) {
        ManagementPackDetails mpack = new ManagementPackDetails();
        mpack.setMpackUrl(source.getMpackUrl());
        mpack.setForce(source.isForce());
        mpack.setPurge(source.isPurge());
        mpack.setPurgeList(source.getPurgeList());
        mpack.setStackDefault(source.isStackDefault());
        mpack.setPreInstalled(source.isPreInstalled());
        return mpack;
    }
}
