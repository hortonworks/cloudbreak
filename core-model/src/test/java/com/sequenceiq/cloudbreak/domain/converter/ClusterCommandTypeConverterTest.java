package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;

public class ClusterCommandTypeConverterTest extends DefaultEnumConverterBaseTest<ClusterCommandType> {

    @Override
    public ClusterCommandType getDefaultValue() {
        return ClusterCommandType.IMPORT_CLUSTER;
    }

    @Override
    public AttributeConverter<ClusterCommandType, String> getVictim() {
        return new ClusterCommandTypeConverter();
    }
}