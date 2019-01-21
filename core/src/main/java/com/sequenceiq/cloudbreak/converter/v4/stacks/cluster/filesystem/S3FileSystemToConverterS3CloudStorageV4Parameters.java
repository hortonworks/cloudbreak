package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.s3.S3CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.S3FileSystem;

@Component
public class S3FileSystemToConverterS3CloudStorageV4Parameters
        extends AbstractConversionServiceAwareConverter<S3FileSystem, S3CloudStorageParametersV4> {

    @Override
    public S3CloudStorageParametersV4 convert(S3FileSystem source) {
        S3CloudStorageParametersV4 fileSystemConfigurations = new S3CloudStorageParametersV4();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
