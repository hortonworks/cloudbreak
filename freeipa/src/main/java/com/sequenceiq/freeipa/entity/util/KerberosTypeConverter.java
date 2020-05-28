package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.kerberos.model.KerberosType;

public class KerberosTypeConverter extends DefaultEnumConverter<KerberosType> {

    @Override
    public KerberosType getDefault() {
        return KerberosType.FREEIPA;
    }
}
