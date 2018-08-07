package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsFileSystem;

@Component
public class AdlsCloudStorageParametersToAdlsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<AdlsCloudStorageParameters, AdlsFileSystem> {

    @Override
    public AdlsFileSystem convert(AdlsCloudStorageParameters source) {
        AdlsFileSystem fileSystemConfigurations = new AdlsFileSystem();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
