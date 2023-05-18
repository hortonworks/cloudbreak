package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Map;

public interface RotationContextProvider {

    <C extends RotationContext> Map<SecretRotationStep, C> getContexts(String resourceId);

    SecretType getSecret();
}
