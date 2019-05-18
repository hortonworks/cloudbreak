package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsFileSystem;

@Component
public class AdlsFileSystemToCloudAdlsView extends AbstractConversionServiceAwareConverter<AdlsFileSystem, CloudAdlsView> {
    @Override
    public CloudAdlsView convert(AdlsFileSystem source) {
        CloudAdlsView cloudAdlsView = new CloudAdlsView();
        cloudAdlsView.setTenantId(source.getTenantId());
        cloudAdlsView.setCredential(source.getCredential());
        cloudAdlsView.setAccountName(source.getAccountName());
        cloudAdlsView.setClientId(source.getClientId());
        return cloudAdlsView;
    }
}
