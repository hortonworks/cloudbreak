package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class WasbCloudStorageParametersV4ToCloudWasbViewConverter extends AbstractConversionServiceAwareConverter<WasbCloudStorageParametersV4, CloudWasbView> {
    @Override
    public CloudWasbView convert(WasbCloudStorageParametersV4 source) {
        CloudWasbView cloudWasbView = new CloudWasbView();
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setAccountName(source.getAccountName());
        return cloudWasbView;
    }
}
