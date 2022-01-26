package com.sequenceiq.cloudbreak.util.password;

import org.springframework.stereotype.Component;

@Component
public class DefaultPasswordGenerator {

    public String generate() {
        return PasswordGenerator.getDefault().generate();
    }
}
