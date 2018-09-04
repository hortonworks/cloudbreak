package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAbfsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AbfsFileSystemToCloudAbfsView
        extends AbstractConversionServiceAwareConverter<AbfsFileSystem, CloudAbfsView> {
    @Override
    public CloudAbfsView convert(AbfsFileSystem source) {
        CloudAbfsView cloudAbfsView = new CloudAbfsView();
        cloudAbfsView.setAccountName(source.getAccountName());
        cloudAbfsView.setAccountKey(source.getAccountKey());
        cloudAbfsView.setResourceGroupName(source.getStorageContainerName());
        return cloudAbfsView;
    }
}
