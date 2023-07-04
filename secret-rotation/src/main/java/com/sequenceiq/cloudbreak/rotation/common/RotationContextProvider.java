package com.sequenceiq.cloudbreak.rotation.common;

import java.util.Map;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface RotationContextProvider {

    <C extends RotationContext> Map<SecretRotationStep, C> getContexts(String resourceId);

    SecretType getSecret();
}
