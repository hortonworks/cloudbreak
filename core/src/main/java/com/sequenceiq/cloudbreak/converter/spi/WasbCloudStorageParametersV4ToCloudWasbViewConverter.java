package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class WasbCloudStorageParametersV4ToCloudWasbViewConverter extends AbstractConversionServiceAwareConverter<WasbCloudStorageParameters, CloudWasbView> {
    @Override
    public CloudWasbView convert(WasbCloudStorageParameters source) {
        CloudWasbView cloudWasbView = new CloudWasbView();
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setAccountName(source.getAccountName());
        cloudWasbView.setSecure(source.isSecure());
        return cloudWasbView;
    }
}
