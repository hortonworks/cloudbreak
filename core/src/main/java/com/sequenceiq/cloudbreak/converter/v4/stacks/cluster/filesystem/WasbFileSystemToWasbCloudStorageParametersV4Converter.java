package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.WasbCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.WasbFileSystem;

@Component
public class WasbFileSystemToWasbCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<WasbFileSystem, WasbCloudStorageV4Parameters> {

    @Override
    public WasbCloudStorageV4Parameters convert(WasbFileSystem source) {
        WasbCloudStorageV4Parameters fileSystemConfigurations = new WasbCloudStorageV4Parameters();
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setAccountKey(source.getAccountKey());
        return fileSystemConfigurations;
    }
}
