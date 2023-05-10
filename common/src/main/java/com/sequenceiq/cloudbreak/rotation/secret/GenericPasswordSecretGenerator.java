package com.sequenceiq.cloudbreak.rotation.secret;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class GenericPasswordSecretGenerator implements SecretGenerator {

    @Override
    public String generate() {
        return PasswordUtil.generatePassword();
    }
}
