package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.GcsFileSystem;

@Component
public class GcsFileSystemToGcsCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<GcsFileSystem, GcsCloudStorageV4Parameters> {

    @Override
    public GcsCloudStorageV4Parameters convert(GcsFileSystem source) {
        GcsCloudStorageV4Parameters fileSystemConfigurations = new GcsCloudStorageV4Parameters();
        fileSystemConfigurations.setServiceAccountEmail(source.getServiceAccountEmail());
        return fileSystemConfigurations;
    }
}
