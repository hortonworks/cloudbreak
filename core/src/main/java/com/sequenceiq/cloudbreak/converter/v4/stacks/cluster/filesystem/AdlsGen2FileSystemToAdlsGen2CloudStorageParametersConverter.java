package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.services.filesystem.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsGen2FileSystemToAdlsGen2CloudStorageParametersConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2FileSystem, AdlsGen2CloudStorageParametersV4> {

    @Override
    public AdlsGen2CloudStorageParametersV4 convert(AdlsGen2FileSystem source) {
        AdlsGen2CloudStorageParametersV4 adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageParametersV4();
        adlsGen2CloudStorageParameters.setAccountName(source.getAccountName());
        adlsGen2CloudStorageParameters.setAccountKey(source.getAccountKey());
        return adlsGen2CloudStorageParameters;
    }
}
