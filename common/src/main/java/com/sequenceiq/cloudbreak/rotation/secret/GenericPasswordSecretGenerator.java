package com.sequenceiq.cloudbreak.rotation.secret;

import com.sequenceiq.cloudbreak.util.PasswordUtil;

public class GenericPasswordSecretGenerator implements SecretGenerator {

    private GenericPasswordSecretGenerator() {

    }

    public static GenericPasswordSecretGenerator of() {
        return new GenericPasswordSecretGenerator();
    }

    @Override
    public String generate() {
        return PasswordUtil.generatePassword();
    }
}
