package com.sequenceiq.periscope.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.ClusterState;

public class ClusterStateConverterTest extends DefaultEnumConverterBaseTest<ClusterState> {

    @Override
    public ClusterState getDefaultValue() {
        return ClusterState.RUNNING;
    }

    @Override
    public AttributeConverter<ClusterState, String> getVictim() {
        return new ClusterStateConverter();
    }
}