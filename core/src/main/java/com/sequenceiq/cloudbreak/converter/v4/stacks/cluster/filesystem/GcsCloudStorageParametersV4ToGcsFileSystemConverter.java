package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;

@Component
public class GcsCloudStorageParametersV4ToGcsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageV4Parameters, GcsFileSystem> {

    @Override
    public GcsFileSystem convert(GcsCloudStorageV4Parameters source) {
        GcsFileSystem fileSystemConfigurations = new GcsFileSystem();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
