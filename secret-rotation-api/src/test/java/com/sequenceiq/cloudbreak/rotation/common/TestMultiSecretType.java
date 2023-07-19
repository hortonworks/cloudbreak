package com.sequenceiq.cloudbreak.rotation.common;

import java.util.Map;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum TestMultiSecretType implements MultiSecretType {
    MULTI_TEST,
    MULTI_TEST_2;

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return TestMultiSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }

    @Override
    public SecretType parentSecretType() {
        return TestSecretType.TEST_2;
    }

    @Override
    public CrnResourceDescriptor parentCrnDescriptor() {
        return CrnResourceDescriptor.DATALAKE;
    }

    @Override
    public Map<CrnResourceDescriptor, SecretType> childSecretTypesByDescriptor() {
        return Map.of(CrnResourceDescriptor.DATAHUB, TestSecretType.TEST_4);
    }
}
