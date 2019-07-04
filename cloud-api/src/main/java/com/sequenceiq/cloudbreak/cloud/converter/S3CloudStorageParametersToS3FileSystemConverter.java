package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.S3FileSystem;

@Component
public class S3CloudStorageParametersToS3FileSystemConverter
        extends AbstractConversionServiceAwareConverter<S3CloudStorageParameters, S3FileSystem> {

    @Override
    public S3FileSystem convert(S3CloudStorageParameters source) {
        S3FileSystem fileSystemConfigurations = new S3FileSystem();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
