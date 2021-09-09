package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;

@Component
public class GcsFileSystemToGcsCloudStorageParametersV4Converter {

    public GcsCloudStorageV1Parameters convert(GcsFileSystem source) {
        GcsCloudStorageV1Parameters fileSystemConfigurations = new GcsCloudStorageV1Parameters();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
