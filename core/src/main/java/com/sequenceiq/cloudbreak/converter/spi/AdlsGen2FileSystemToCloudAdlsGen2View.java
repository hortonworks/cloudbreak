package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2FileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AdlsGen2FileSystemToCloudAdlsGen2View
        extends AbstractConversionServiceAwareConverter<AdlsGen2FileSystem, CloudAdlsGen2View> {
    @Override
    public CloudAdlsGen2View convert(AdlsGen2FileSystem source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View();
        cloudAdlsGen2View.setAccountName(source.getAccountName());
        cloudAdlsGen2View.setAccountKey(source.getAccountKey());
        cloudAdlsGen2View.setResourceGroupName(source.getStorageContainerName());
        return cloudAdlsGen2View;
    }
}
