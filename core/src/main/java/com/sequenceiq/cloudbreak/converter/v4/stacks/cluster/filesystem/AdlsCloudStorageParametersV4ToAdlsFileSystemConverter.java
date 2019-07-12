package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.common.api.cloudstorage.AdlsCloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsFileSystem;

@Component
public class AdlsCloudStorageParametersV4ToAdlsFileSystemConverter
        extends AbstractConversionServiceAwareConverter<AdlsCloudStorageV1Parameters, AdlsFileSystem> {

    @Override
    public AdlsFileSystem convert(AdlsCloudStorageV1Parameters source) {
        AdlsFileSystem fileSystemConfigurations = new AdlsFileSystem();
        fileSystemConfigurations.setClientId(source.getClientId());
        fileSystemConfigurations.setAccountName(source.getAccountName());
        fileSystemConfigurations.setCredential(source.getCredential());
        fileSystemConfigurations.setTenantId(source.getTenantId());
        return fileSystemConfigurations;
    }
}
