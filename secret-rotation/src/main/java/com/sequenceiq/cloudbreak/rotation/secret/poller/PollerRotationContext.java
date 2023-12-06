package com.sequenceiq.cloudbreak.rotation.secret.poller;

import java.util.Map;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class PollerRotationContext extends RotationContext {

    private final SecretType secretType;

    private final Map<String, String> additionalProperties;

    public PollerRotationContext(String resourceCrn, SecretType secretType) {
        super(resourceCrn);
        this.secretType = secretType;
        this.additionalProperties = Maps.newHashMap();
    }

    public PollerRotationContext(String datalakeCrn, SecretType secretType, Map<String, String> additionalProperties) {
        super(datalakeCrn);
        this.secretType = secretType;
        this.additionalProperties = additionalProperties;
    }

    public SecretType getSecretType() {
        return secretType;
    }

    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
}