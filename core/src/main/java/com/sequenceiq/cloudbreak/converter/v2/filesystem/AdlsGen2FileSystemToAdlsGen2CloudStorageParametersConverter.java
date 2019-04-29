package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2FileSystem, AdlsGen2CloudStorageParameters> {

    @Override
    public AdlsGen2CloudStorageParameters convert(AdlsGen2FileSystem source) {
        AdlsGen2CloudStorageParameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageParameters();
        adlsGen2CloudStorageParameters.setAccountName(source.getAccountName());
        adlsGen2CloudStorageParameters.setAccountKey(source.getAccountKey());
        adlsGen2CloudStorageParameters.setSecure(source.isSecure());
        return adlsGen2CloudStorageParameters;
    }
}
