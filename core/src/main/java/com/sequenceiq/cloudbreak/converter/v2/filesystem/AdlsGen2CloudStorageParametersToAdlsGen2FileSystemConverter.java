package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

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
