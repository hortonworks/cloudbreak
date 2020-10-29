package com.sequenceiq.redbeams.repository.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateType;

public class SslCertificateTypeConverter extends DefaultEnumConverter<SslCertificateType> {

    @Override
    public SslCertificateType getDefault() {
        return SslCertificateType.NONE;
    }
}
