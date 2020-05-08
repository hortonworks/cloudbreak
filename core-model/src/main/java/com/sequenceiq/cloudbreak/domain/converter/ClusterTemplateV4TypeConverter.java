package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class ClusterTemplateV4TypeConverter extends DefaultEnumConverter<ClusterTemplateV4Type> {

    @Override
    public ClusterTemplateV4Type getDefault() {
        return ClusterTemplateV4Type.OTHER;
    }
}
