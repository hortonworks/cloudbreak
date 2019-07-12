package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.common.api.cloudstorage.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.S3FileSystem;

@Component
public class S3CloudStorageParametersV4ToS3FileSystemConverter
        extends AbstractConversionServiceAwareConverter<S3CloudStorageV1Parameters, S3FileSystem> {

    @Override
    public S3FileSystem convert(S3CloudStorageV1Parameters source) {
        S3FileSystem fileSystemConfigurations = new S3FileSystem();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
