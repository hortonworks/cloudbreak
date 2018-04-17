package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Component
public class ManagementPackComponentToClusterResponseMpackDetails extends
        AbstractConversionServiceAwareConverter<ManagementPackComponent, ManagementPackDetails> {
    @Override
    public ManagementPackDetails convert(ManagementPackComponent source) {
        ManagementPackDetails mpack = new ManagementPackDetails();
        mpack.setName(source.getName());
        return mpack;
    }
}
