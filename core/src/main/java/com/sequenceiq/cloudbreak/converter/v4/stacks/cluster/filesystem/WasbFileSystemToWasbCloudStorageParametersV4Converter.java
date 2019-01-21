package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;

@Component
public class WasbFileSystemToWasbCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<WasbFileSystem, WasbCloudStorageParametersV4> {

    @Override
    public WasbCloudStorageParametersV4 convert(WasbFileSystem source) {
        WasbCloudStorageParametersV4 fileSystemConfigurations = new WasbCloudStorageParametersV4();
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setAccountKey(source.getAccountKey());
        return fileSystemConfigurations;
    }
}
