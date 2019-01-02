package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsFileSystem;

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
