package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class CloudPlatformConverter extends DefaultEnumConverter<CloudPlatform> {

    @Override
    public CloudPlatform getDefault() {
        return null;
    }

}
