package com.sequenceiq.periscope.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class ClusterManagerVariantConverterTest extends DefaultEnumConverterBaseTest<ClusterManagerVariant> {

    @Override
    public ClusterManagerVariant getDefaultValue() {
        return ClusterManagerVariant.CLOUDERA_MANAGER;
    }

    @Override
    public AttributeConverter<ClusterManagerVariant, String> getVictim() {
        return new ClusterManagerVariantConverter();
    }
}
