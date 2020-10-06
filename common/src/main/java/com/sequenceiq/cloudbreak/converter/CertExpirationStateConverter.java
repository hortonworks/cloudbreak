package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.CertExpirationState;

public class CertExpirationStateConverter extends DefaultEnumConverter<CertExpirationState> {
    @Override
    public CertExpirationState getDefault() {
        return CertExpirationState.VALID;
    }
}
