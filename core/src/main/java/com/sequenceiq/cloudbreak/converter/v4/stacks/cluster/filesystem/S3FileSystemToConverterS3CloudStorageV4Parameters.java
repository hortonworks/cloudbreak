package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.S3FileSystem;

@Component
public class S3FileSystemToConverterS3CloudStorageV4Parameters {

    public S3CloudStorageV1Parameters convert(S3FileSystem source) {
        S3CloudStorageV1Parameters fileSystemConfigurations = new S3CloudStorageV1Parameters();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
