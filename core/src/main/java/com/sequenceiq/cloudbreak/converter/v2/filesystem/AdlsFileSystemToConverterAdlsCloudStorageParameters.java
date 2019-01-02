package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsFileSystem;

@Component
public class AdlsFileSystemToConverterAdlsCloudStorageParameters
        extends AbstractConversionServiceAwareConverter<AdlsFileSystem, AdlsCloudStorageParameters> {

    @Override
    public AdlsCloudStorageParameters convert(AdlsFileSystem source) {
        AdlsCloudStorageParameters fileSystemConfigurations = new AdlsCloudStorageParameters();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
