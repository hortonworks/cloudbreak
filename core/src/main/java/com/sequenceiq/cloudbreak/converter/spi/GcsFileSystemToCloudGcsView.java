package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class GcsFileSystemToCloudGcsView
        extends AbstractConversionServiceAwareConverter<GcsFileSystem, CloudGcsView> {
    @Override
    public CloudGcsView convert(GcsFileSystem source) {
        CloudGcsView cloudGcsView = new CloudGcsView();
        cloudGcsView.setServiceAccountEmail(source.getServiceAccountEmail());
        return cloudGcsView;
    }
}
