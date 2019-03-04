package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;

@Component
public class AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2FileSystem, AdlsGen2CloudStorageV4Parameters> {

    @Override
    public AdlsGen2CloudStorageV4Parameters convert(AdlsGen2FileSystem source) {
        AdlsGen2CloudStorageV4Parameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageV4Parameters();
        adlsGen2CloudStorageParameters.setAccountName(source.getAccountName());
        adlsGen2CloudStorageParameters.setAccountKey(source.getAccountKey());
        return adlsGen2CloudStorageParameters;
    }
}
