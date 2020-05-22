package com.sequenceiq.periscope.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

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