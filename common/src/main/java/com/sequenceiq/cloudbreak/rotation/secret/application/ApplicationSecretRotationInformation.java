package com.sequenceiq.cloudbreak.rotation.secret.application;

import java.util.Set;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public interface ApplicationSecretRotationInformation {

    Set<Class<? extends SecretType>> supportedSecretTypes();

}
