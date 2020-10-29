package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;

public class RdsSslModeConverter extends DefaultEnumConverter<RdsSslMode> {

    @Override
    public RdsSslMode getDefault() {
        return RdsSslMode.DISABLED;
    }

}
