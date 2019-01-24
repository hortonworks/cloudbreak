package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.GcsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class GcsCloudStorageParametersV4ToCloudGcsView
        extends AbstractConversionServiceAwareConverter<GcsCloudStorageV4Parameters, CloudGcsView> {
    @Override
    public CloudGcsView convert(GcsCloudStorageV4Parameters source) {
        CloudGcsView cloudGcsView = new CloudGcsView();
        cloudGcsView.setServiceAccountEmail(source.getServiceAccountEmail());
        return cloudGcsView;
    }
}
