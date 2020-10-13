package com.sequenceiq.redbeams.repository.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;

public class SslCertificateTypeConverterTest extends DefaultEnumConverterBaseTest<SslCertificateType> {

    @Override
    public SslCertificateType getDefaultValue() {
        return SslCertificateType.NONE;
    }

    @Override
    public AttributeConverter<SslCertificateType, String> getVictim() {
        return new SslCertificateTypeConverter();
    }
}