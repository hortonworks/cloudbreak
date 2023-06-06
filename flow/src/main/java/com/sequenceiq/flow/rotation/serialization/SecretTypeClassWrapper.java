package com.sequenceiq.flow.rotation.serialization;

import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public record SecretTypeClassWrapper(Class<Enum<? extends SecretType>> clazz, String value) {

}
