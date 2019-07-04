package com.sequenceiq.cloudbreak.cloud.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsGen2FileSystem;

@Component
public class AdlsGen2CloudStorageParametersToAdlsGen2FileSystemConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2CloudStorageParameters, AdlsGen2FileSystem> {

    @Override
    public AdlsGen2FileSystem convert(AdlsGen2CloudStorageParameters source) {
        AdlsGen2FileSystem adlsGen2FileSystem = new AdlsGen2FileSystem();
        adlsGen2FileSystem.setAccountName(source.getAccountName());
        adlsGen2FileSystem.setAccountKey(source.getAccountKey());
        return adlsGen2FileSystem;
    }
}
