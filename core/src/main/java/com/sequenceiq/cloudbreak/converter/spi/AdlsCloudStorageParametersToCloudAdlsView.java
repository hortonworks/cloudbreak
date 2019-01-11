package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsCloudStorageParametersToCloudAdlsView
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
