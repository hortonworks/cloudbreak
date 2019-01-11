package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsGen2CloudStorageParametersToCloudAdlsGen2ViewConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2CloudStorageParameters, CloudAdlsGen2View> {
    @Override
    public CloudAdlsGen2View convert(AdlsGen2CloudStorageParameters source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View();
        cloudAdlsGen2View.setAccountKey(source.getAccountKey());
        cloudAdlsGen2View.setAccountName(source.getAccountName());
        return cloudAdlsGen2View;
    }
}
