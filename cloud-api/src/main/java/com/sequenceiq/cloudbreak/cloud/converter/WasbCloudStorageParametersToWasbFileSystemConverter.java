package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.WasbFileSystem;

@Component
public class WasbCloudStorageParametersToWasbFileSystemConverter
        extends AbstractConversionServiceAwareConverter<WasbCloudStorageParameters, WasbFileSystem> {

    @Override
    public WasbFileSystem convert(WasbCloudStorageParameters source) {
        WasbFileSystem fileSystemConfigurations = new WasbFileSystem();
        fileSystemConfigurations.setSecure(source.isSecure());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setAccountKey(source.getAccountKey());
        return fileSystemConfigurations;
    }
}
