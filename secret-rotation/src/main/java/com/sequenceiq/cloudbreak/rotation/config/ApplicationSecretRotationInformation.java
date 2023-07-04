package com.sequenceiq.cloudbreak.rotation.config;

import com.sequenceiq.cloudbreak.rotation.SecretType;

public interface ApplicationSecretRotationInformation {

    Class<? extends SecretType> supportedSecretType();

}
