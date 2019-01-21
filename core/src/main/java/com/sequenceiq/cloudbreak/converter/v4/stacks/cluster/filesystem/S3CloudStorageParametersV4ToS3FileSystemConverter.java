package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;

@Component
public class S3CloudStorageParametersV4ToS3FileSystemConverter
        extends AbstractConversionServiceAwareConverter<S3CloudStorageParametersV4, S3FileSystem> {

    @Override
    public S3FileSystem convert(S3CloudStorageParametersV4 source) {
        S3FileSystem fileSystemConfigurations = new S3FileSystem();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
