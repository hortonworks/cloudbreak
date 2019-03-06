package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;

@Component
public class AdlsCloudStorageParametersV4ToAdlsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<AdlsCloudStorageV4Parameters, AdlsFileSystem> {

    @Override
    public AdlsFileSystem convert(AdlsCloudStorageV4Parameters source) {
        AdlsFileSystem fileSystemConfigurations = new AdlsFileSystem();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
