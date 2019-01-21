package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.azure.AdlsGen2CloudStorageParametersV4;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsGen2CloudStorageParametersV4ToCloudAdlsGen2ViewConverter
        extends AbstractConversionServiceAwareConverter<AdlsGen2CloudStorageParametersV4, CloudAdlsGen2View> {
    @Override
    public CloudAdlsGen2View convert(AdlsGen2CloudStorageParametersV4 source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View();
        cloudAdlsGen2View.setAccountKey(source.getAccountKey());
        cloudAdlsGen2View.setAccountName(source.getAccountName());
        return cloudAdlsGen2View;
    }
}
