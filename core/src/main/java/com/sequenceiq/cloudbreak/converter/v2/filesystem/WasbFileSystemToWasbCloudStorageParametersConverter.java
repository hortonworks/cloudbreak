package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.wasb.WasbFileSystem;

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
