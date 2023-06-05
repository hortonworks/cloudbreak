package com.sequenceiq.cloudbreak.rotation.secret.application;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public interface ApplicationSecretRotationInformation {

    Class<? extends SecretType> supportedSecretType();

}
