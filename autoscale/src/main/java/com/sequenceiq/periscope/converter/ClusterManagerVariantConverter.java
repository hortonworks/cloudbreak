package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ClusterManagerVariantConverter extends DefaultEnumConverter<ClusterManagerVariant> {

    @Override
    public ClusterManagerVariant getDefault() {
        return ClusterManagerVariant.CLOUDERA_MANAGER;
    }
}
