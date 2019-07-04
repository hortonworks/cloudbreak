package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.WasbFileSystem;

@Component
public class WasbFileSystemToWasbCloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<WasbFileSystem, WasbCloudStorageParameters> {

    @Override
    public WasbCloudStorageParameters convert(WasbFileSystem source) {
        WasbCloudStorageParameters fileSystemConfigurations = new WasbCloudStorageParameters();
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setAccountKey(source.getAccountKey());
        return fileSystemConfigurations;
    }
}
