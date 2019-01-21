package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;

@Component
public class GcsFileSystemToGcsCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<GcsFileSystem, GcsCloudStorageParametersV4> {

    @Override
    public GcsCloudStorageParametersV4 convert(GcsFileSystem source) {
        GcsCloudStorageParametersV4 fileSystemConfigurations = new GcsCloudStorageParametersV4();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
