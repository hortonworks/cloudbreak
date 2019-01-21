package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsCloudStorageParametersV4;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsFileSystem;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsFileSystemToAdlsCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<AdlsFileSystem, AdlsCloudStorageParametersV4> {

    @Override
    public AdlsCloudStorageParametersV4 convert(AdlsFileSystem source) {
        AdlsCloudStorageParametersV4 fileSystemConfigurations = new AdlsCloudStorageParametersV4();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
