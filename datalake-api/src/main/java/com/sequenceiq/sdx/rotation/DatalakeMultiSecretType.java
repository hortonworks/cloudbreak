package com.sequenceiq.sdx.rotation;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum DatalakeMultiSecretType implements MultiSecretType {

    CM_SERVICE_SHARED_DB;

    @Override
    public SecretType parentSecretType() {
        return DatalakeSecretType.DL_CM_SERVICE_SHARED_DB;
    }

    @Override
    public SecretType childSecretType() {
        return DatalakeSecretType.DH_CM_SERVICE_SHARED_DB;
    }

    @Override
    public CrnResourceDescriptor parentCrnDescriptor() {
        return CrnResourceDescriptor.DATALAKE;
    }

    @Override
    public CrnResourceDescriptor childCrnDescriptor() {
        return CrnResourceDescriptor.DATAHUB;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return DatalakeMultiSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
