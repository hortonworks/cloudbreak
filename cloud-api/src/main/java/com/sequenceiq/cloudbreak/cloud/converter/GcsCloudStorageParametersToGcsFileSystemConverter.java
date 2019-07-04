package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;

@Component
public class GcsCloudStorageParametersToGcsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageParameters, GcsFileSystem> {

    @Override
    public GcsFileSystem convert(GcsCloudStorageParameters source) {
        GcsFileSystem fileSystemConfigurations = new GcsFileSystem();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
