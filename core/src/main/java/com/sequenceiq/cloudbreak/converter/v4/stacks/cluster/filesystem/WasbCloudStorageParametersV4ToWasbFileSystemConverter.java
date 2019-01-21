package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.WasbCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.WasbFileSystem;

@Component
public class WasbCloudStorageParametersV4ToWasbFileSystemConverter
        extends AbstractConversionServiceAwareConverter<WasbCloudStorageParametersV4, WasbFileSystem> {

    @Override
    public WasbFileSystem convert(WasbCloudStorageParametersV4 source) {
        WasbFileSystem fileSystemConfigurations = new WasbFileSystem();
        fileSystemConfigurations.setSecure(source.isSecure());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setAccountKey(source.getAccountKey());
        return fileSystemConfigurations;
    }
}
