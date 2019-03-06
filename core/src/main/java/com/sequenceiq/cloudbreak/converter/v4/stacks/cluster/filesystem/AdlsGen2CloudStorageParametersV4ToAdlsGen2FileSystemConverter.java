package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsGen2CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;

@Component
public class AdlsGen2CloudStorageParametersV4ToAdlsGen2FileSystemConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2CloudStorageV4Parameters, AdlsGen2FileSystem> {

    @Override
    public AdlsGen2FileSystem convert(AdlsGen2CloudStorageV4Parameters source) {
        AdlsGen2FileSystem adlsGen2FileSystem = new AdlsGen2FileSystem();
        adlsGen2FileSystem.setAccountName(source.getAccountName());
        adlsGen2FileSystem.setAccountKey(source.getAccountKey());
        return adlsGen2FileSystem;
    }
}
