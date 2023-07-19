package com.sequenceiq.freeipa.service.rotation;

import java.util.Map;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

public enum TestFreeipaMultiSecretType implements MultiSecretType {
    IPA_MULTI_SECRET;

    @Override
    public SecretType parentSecretType() {
        return TestFreeipaSecretType.IPA_SECRET_1;
    }

    @Override
    public CrnResourceDescriptor parentCrnDescriptor() {
        return CrnResourceDescriptor.ENVIRONMENT;
    }

    @Override
    public Map<CrnResourceDescriptor, SecretType> childSecretTypesByDescriptor() {
        return Map.of(CrnResourceDescriptor.DATALAKE, DatalakeSecretType.DATALAKE_DEMO_SECRET,
                CrnResourceDescriptor.DATAHUB, CloudbreakSecretType.DATAHUB_DEMO_SECRET);
    }

    @Override
    public Class<? extends Enum<?>> getClazz() {
        return TestFreeipaMultiSecretType.class;
    }

    @Override
    public String value() {
        return name();
    }
}
