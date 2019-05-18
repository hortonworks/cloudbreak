package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;

@Component
public class S3FileSystemToConverterS3CloudStorageV4Parameters
        extends AbstractConversionServiceAwareConverter<S3FileSystem, S3CloudStorageV4Parameters> {

    @Override
    public S3CloudStorageV4Parameters convert(S3FileSystem source) {
        S3CloudStorageV4Parameters fileSystemConfigurations = new S3CloudStorageV4Parameters();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
