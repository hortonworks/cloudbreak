package com.sequenceiq.cloudbreak.rotation.common;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_2;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public enum TestMultiSecretType implements MultiSecretType {
    MULTI_TEST;

    @Override
    public SecretType parentSecretType() {
        return TEST_2;
    }

    @Override
    public SecretType childSecretType() {
        return TEST_2;
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
        return TestMultiSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
