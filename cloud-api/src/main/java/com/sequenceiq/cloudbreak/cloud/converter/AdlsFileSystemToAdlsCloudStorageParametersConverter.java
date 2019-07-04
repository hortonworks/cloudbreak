package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsFileSystem;

@Component
public class AdlsFileSystemToAdlsCloudStorageParametersConverter
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
