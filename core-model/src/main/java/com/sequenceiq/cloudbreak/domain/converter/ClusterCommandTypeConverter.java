package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;

public class ClusterCommandTypeConverter extends DefaultEnumConverter<ClusterCommandType> {

    @Override
    public ClusterCommandType getDefault() {
        return ClusterCommandType.IMPORT_CLUSTER;
    }
}
