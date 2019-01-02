package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.s3.S3CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class S3CloudStorageParametersToCloudS3View
        extends AbstractConversionServiceAwareConverter<S3CloudStorageParameters, CloudS3View> {
    @Override
    public CloudS3View convert(S3CloudStorageParameters source) {
        CloudS3View cloudS3View = new CloudS3View();
        cloudS3View.setInstanceProfile(source.getInstanceProfile());
        return cloudS3View;
    }
}
