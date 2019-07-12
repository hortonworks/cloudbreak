package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.GcsFileSystem;

@Component
public class GcsCloudStorageParametersV4ToGcsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageV1Parameters, GcsFileSystem> {

    @Override
    public GcsFileSystem convert(GcsCloudStorageV1Parameters source) {
        GcsFileSystem fileSystemConfigurations = new GcsFileSystem();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
