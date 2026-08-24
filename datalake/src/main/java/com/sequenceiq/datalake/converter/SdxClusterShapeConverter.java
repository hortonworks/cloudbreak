package com.sequenceiq.datalake.converter;

import java.util.Optional;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class SdxClusterShapeConverter extends DefaultEnumConverter<SdxClusterShape> {

    @Override
    public SdxClusterShape getDefault() {
        return SdxClusterShape.LIGHT_DUTY;
    }

    @Override
    public Optional<SdxClusterShape> tryConvertUnknownField(String attribute) {
        if ("LIGHT_DUTY_WITHOUT_HBASE".equals(attribute)) {
            return Optional.of(SdxClusterShape.LIGHT_DUTY_PRO);
        }
        if ("ENTERPRISE_WITHOUT_HBASE".equals(attribute)) {
            return Optional.of(SdxClusterShape.ENTERPRISE_PRO);
        }
        return Optional.empty();
    }
}
