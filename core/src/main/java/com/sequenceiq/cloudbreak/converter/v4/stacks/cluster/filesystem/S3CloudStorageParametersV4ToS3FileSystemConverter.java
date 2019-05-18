package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;

@Component
public class S3CloudStorageParametersV4ToS3FileSystemConverter
        extends AbstractConversionServiceAwareConverter<S3CloudStorageV4Parameters, S3FileSystem> {

    @Override
    public S3FileSystem convert(S3CloudStorageV4Parameters source) {
        S3FileSystem fileSystemConfigurations = new S3FileSystem();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
