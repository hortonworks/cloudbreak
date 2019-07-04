package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;

@Component
public class GcsFileSystemToGcsCloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<GcsFileSystem, GcsCloudStorageParameters> {

    @Override
    public GcsCloudStorageParameters convert(GcsFileSystem source) {
        GcsCloudStorageParameters fileSystemConfigurations = new GcsCloudStorageParameters();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
