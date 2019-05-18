package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsGen2FileSystem;

@Component
public class AdlsGen2FileSystemToCloudAdlsGen2View
        extends AbstractConversionServiceAwareConverter<AdlsGen2FileSystem, CloudAdlsGen2View> {
    @Override
    public CloudAdlsGen2View convert(AdlsGen2FileSystem source) {
        CloudAdlsGen2View cloudAdlsGen2View = new CloudAdlsGen2View();
        cloudAdlsGen2View.setAccountName(source.getAccountName());
        cloudAdlsGen2View.setAccountKey(source.getAccountKey());
        cloudAdlsGen2View.setResourceGroupName(source.getStorageContainerName());
        cloudAdlsGen2View.setSecure(source.isSecure());
        return cloudAdlsGen2View;
    }
}
