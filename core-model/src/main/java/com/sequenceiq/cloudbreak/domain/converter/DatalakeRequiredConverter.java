package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class DatalakeRequiredConverter extends DefaultEnumConverter<DatalakeRequired> {
    @Override
    public DatalakeRequired getDefault() {
        return DatalakeRequired.OPTIONAL;
    }
}
