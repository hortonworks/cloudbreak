package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CertExpirationState;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class CertExpirationStateConverter extends DefaultEnumConverter<CertExpirationState> {
    @Override
    public CertExpirationState getDefault() {
        return CertExpirationState.VALID;
    }
}
