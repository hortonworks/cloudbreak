package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.gcs.GcsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.GcsFileSystem;

@Component
public class GcsCloudStorageParametersV4ToGcsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageParametersV4, GcsFileSystem> {

    @Override
    public GcsFileSystem convert(GcsCloudStorageParametersV4 source) {
        GcsFileSystem fileSystemConfigurations = new GcsFileSystem();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
