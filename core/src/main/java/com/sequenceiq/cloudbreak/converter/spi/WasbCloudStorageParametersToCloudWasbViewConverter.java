package com.sequenceiq.cloudbreak.converter.spi;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import org.springframework.stereotype.Component;

@Component
public class WasbCloudStorageParametersToCloudWasbViewConverter
        extends AbstractConversionServiceAwareConverter<WasbCloudStorageParameters, CloudWasbView> {
    @Override
    public CloudWasbView convert(WasbCloudStorageParameters source) {
        CloudWasbView cloudWasbView = new CloudWasbView();
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setAccountName(source.getAccountName());
        return cloudWasbView;
    }
}
