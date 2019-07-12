package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.common.api.cloudstorage.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;

@Component
public class AdlsFileSystemToAdlsCloudStorageParametersV4Converter
        extends AbstractConversionServiceAwareConverter<AdlsFileSystem, AdlsCloudStorageV1Parameters> {

    @Override
    public AdlsCloudStorageV1Parameters convert(AdlsFileSystem source) {
        AdlsCloudStorageV1Parameters fileSystemConfigurations = new AdlsCloudStorageV1Parameters();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
