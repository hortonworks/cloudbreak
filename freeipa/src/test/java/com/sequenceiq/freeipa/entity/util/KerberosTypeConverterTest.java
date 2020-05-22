package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

import javax.persistence.AttributeConverter;

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