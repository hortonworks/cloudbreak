package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.GcsCloudStorageV1Parameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class GcsCloudStorageParametersV4ToCloudGcsView
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageV1Parameters, CloudGcsView> {
    @Override
    public CloudGcsView convert(GcsCloudStorageV1Parameters source) {
        CloudGcsView cloudGcsView = new CloudGcsView();
        cloudGcsView.setServiceAccountEmail(source.getServiceAccountEmail());
        return cloudGcsView;
    }
}
