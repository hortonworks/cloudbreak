package com.sequenceiq.sdx.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_DEMO_SECRET;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_DEMO_SECRET;

import java.util.Map;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum DatalakeMultiSecretType implements MultiSecretType {

    CM_SERVICE_SHARED_DB(DATALAKE_CM_SERVICE_SHARED_DB, DATAHUB_CM_SERVICE_SHARED_DB),
    DEMO_MULTI_SECRET(DATALAKE_DEMO_SECRET, DATAHUB_DEMO_SECRET);

    private SecretType parentSecret;

    private SecretType childSecret;

    DatalakeMultiSecretType(SecretType parentSecret, SecretType childSecret) {
        this.parentSecret = parentSecret;
        this.childSecret = childSecret;
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return DatalakeMultiSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }

    @Override
    public SecretType parentSecretType() {
        return parentSecret;
    }

    @Override
    public CrnResourceDescriptor parentCrnDescriptor() {
        return CrnResourceDescriptor.DATALAKE;
    }

    @Override
    public Map<CrnResourceDescriptor, SecretType> childSecretTypesByDescriptor() {
        return Map.of(CrnResourceDescriptor.DATAHUB, childSecret);
    }
}
