package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class WasbCloudStorageParametersV4ToCloudWasbViewConverter extends AbstractConversionServiceAwareConverter<WasbCloudStorageV4Parameters, CloudWasbView> {
    @Override
    public CloudWasbView convert(WasbCloudStorageV4Parameters source) {
        CloudWasbView cloudWasbView = new CloudWasbView();
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setAccountName(source.getAccountName());
        cloudWasbView.setSecure(source.isSecure());
        return cloudWasbView;
    }
}
