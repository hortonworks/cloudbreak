package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

public class KerberosTypeConverterTest extends DefaultEnumConverterBaseTest<KerberosType> {

    @Override
    public KerberosType getDefaultValue() {
        return KerberosType.FREEIPA;
    }

    @Override
    public AttributeConverter<KerberosType, String> getVictim() {
        return new KerberosTypeConverter();
    }
}
