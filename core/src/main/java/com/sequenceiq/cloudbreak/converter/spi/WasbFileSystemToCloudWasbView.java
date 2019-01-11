package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudWasbView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class WasbFileSystemToCloudWasbView
        extends AbstractConversionServiceAwareConverter<WasbFileSystem, CloudWasbView> {
    @Override
    public CloudWasbView convert(WasbFileSystem source) {
        CloudWasbView cloudWasbView = new CloudWasbView();
        cloudWasbView.setAccountName(source.getAccountName());
        cloudWasbView.setAccountKey(source.getAccountKey());
        cloudWasbView.setSecure(source.isSecure());
        cloudWasbView.setResourceGroupName(source.getStorageContainerName());
        return cloudWasbView;
    }
}
