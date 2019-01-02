package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class S3CloudStorageParametersV4ToCloudS3View
        extends AbstractConversionServiceAwareConverter<S3CloudStorageV4Parameters, CloudS3View> {
    @Override
    public CloudS3View convert(S3CloudStorageV4Parameters source) {
        CloudS3View cloudS3View = new CloudS3View();
        cloudS3View.setInstanceProfile(source.getInstanceProfile());
        return cloudS3View;
    }
}
