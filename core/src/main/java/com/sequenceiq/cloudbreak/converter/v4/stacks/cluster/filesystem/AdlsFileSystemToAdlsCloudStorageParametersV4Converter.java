package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;

@Component
public class AdlsFileSystemToAdlsCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<AdlsFileSystem, AdlsCloudStorageV4Parameters> {

    @Override
    public AdlsCloudStorageV4Parameters convert(AdlsFileSystem source) {
        AdlsCloudStorageV4Parameters fileSystemConfigurations = new AdlsCloudStorageV4Parameters();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
