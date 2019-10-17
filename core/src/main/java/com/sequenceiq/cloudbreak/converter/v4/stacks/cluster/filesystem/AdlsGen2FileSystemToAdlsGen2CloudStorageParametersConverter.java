package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.filesystem.AdlsGen2FileSystem;

@Component
public class AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2FileSystem, AdlsGen2CloudStorageV1Parameters> {

    @Override
    public AdlsGen2CloudStorageV1Parameters convert(AdlsGen2FileSystem source) {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageParameters.setAccountName(source.getAccountName());
        adlsGen2CloudStorageParameters.setAccountKey(source.getAccountKey());
        adlsGen2CloudStorageParameters.setSecure(source.isSecure());
        adlsGen2CloudStorageParameters.setManagedIdentity(source.getManagedIdentity());
        return adlsGen2CloudStorageParameters;
    }
}
