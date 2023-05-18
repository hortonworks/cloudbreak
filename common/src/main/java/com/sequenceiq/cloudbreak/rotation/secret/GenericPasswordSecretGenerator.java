package com.sequenceiq.cloudbreak.rotation.secret;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class GenericPasswordSecretGenerator implements SecretGenerator {

    @Override
    public String generate(Map<String, Object> arguments) {
        return PasswordUtil.generatePassword();
    }
}
