package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.old.WasbCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.WasbFileSystem;

@Component
public class WasbFileSystemToWasbCloudStorageParametersV4Converter {

    public WasbCloudStorageV1Parameters convert(WasbFileSystem source) {
        WasbCloudStorageV1Parameters fileSystemConfigurations = new WasbCloudStorageV1Parameters();
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setAccountKey(source.getAccountKey());
        return fileSystemConfigurations;
    }
}
