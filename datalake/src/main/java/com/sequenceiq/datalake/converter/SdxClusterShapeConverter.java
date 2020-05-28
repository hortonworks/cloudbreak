package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

public class SdxClusterShapeConverter extends DefaultEnumConverter<SdxClusterShape> {

    @Override
    public SdxClusterShape getDefault() {
        return SdxClusterShape.LIGHT_DUTY;
    }
}
