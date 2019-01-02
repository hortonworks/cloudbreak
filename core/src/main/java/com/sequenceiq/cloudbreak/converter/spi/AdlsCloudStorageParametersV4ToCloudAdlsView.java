package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsCloudStorageParametersV4ToCloudAdlsView
        extends AbstractConversionServiceAwareConverter<AdlsCloudStorageV4Parameters, CloudAdlsView> {
    @Override
    public CloudAdlsView convert(AdlsCloudStorageV4Parameters source) {
        CloudAdlsView cloudAdlsView = new CloudAdlsView();
        cloudAdlsView.setAccountName(source.getAccountName());
        cloudAdlsView.setClientId(source.getClientId());
        cloudAdlsView.setCredential(source.getCredential());
        cloudAdlsView.setTenantId(source.getTenantId());
        return cloudAdlsView;
    }
}
