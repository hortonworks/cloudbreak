package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Map;

public interface RotationContextProvider {

    Map<SecretLocationType, RotationContext> getContexts(String resourceId);

    SecretType getSecret();
}
