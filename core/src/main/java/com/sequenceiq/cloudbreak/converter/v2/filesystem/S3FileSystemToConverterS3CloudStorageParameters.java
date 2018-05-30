package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.api.model.filesystem.S3FileSystem;

@Component
public class S3FileSystemToConverterS3CloudStorageParameters
        extends AbstractConversionServiceAwareConverter<S3FileSystem, S3CloudStorageParameters> {

    @Override
    public S3CloudStorageParameters convert(S3FileSystem source) {
        S3CloudStorageParameters fileSystemConfigurations = new S3CloudStorageParameters();
        fileSystemConfigurations.setInstanceProfile(source.getInstanceProfile());
        return fileSystemConfigurations;
    }
}
