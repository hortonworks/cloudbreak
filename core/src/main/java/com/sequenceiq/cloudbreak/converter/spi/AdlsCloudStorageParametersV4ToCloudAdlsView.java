package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsCloudStorageParametersV4ToCloudAdlsView
        extends AbstractConversionServiceAwareConverter<AdlsCloudStorageParameters, CloudAdlsView> {
    @Override
    public CloudAdlsView convert(AdlsCloudStorageParameters source) {
        CloudAdlsView cloudAdlsView = new CloudAdlsView();
        cloudAdlsView.setAccountName(source.getAccountName());
        cloudAdlsView.setClientId(source.getClientId());
        cloudAdlsView.setCredential(source.getCredential());
        cloudAdlsView.setTenantId(source.getTenantId());
        return cloudAdlsView;
    }
}
