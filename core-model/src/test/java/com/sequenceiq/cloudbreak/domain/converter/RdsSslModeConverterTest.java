package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;

class RdsSslModeConverterTest extends DefaultEnumConverterBaseTest<RdsSslMode> {

    @Override
    public RdsSslMode getDefaultValue() {
        return RdsSslMode.DISABLED;
    }

    @Override
    public AttributeConverter<RdsSslMode, String> getVictim() {
        return new RdsSslModeConverter();
    }

}
