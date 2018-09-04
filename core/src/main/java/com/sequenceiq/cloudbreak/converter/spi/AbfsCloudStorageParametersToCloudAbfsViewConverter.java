package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAbfsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AbfsCloudStorageParametersToCloudAbfsViewConverter
        extends AbstractConversionServiceAwareConverter<AbfsCloudStorageParameters, CloudAbfsView> {
    @Override
    public CloudAbfsView convert(AbfsCloudStorageParameters source) {
        CloudAbfsView cloudAbfsView = new CloudAbfsView();
        cloudAbfsView.setAccountKey(source.getAccountKey());
        cloudAbfsView.setAccountName(source.getAccountName());
        return cloudAbfsView;
    }
}
