package com.sequenceiq.cloudbreak.orchestrator.salt.rotation;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataSecretModifier;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;

@Component
public class SaltBootPasswordUserDataModifier implements UserDataSecretModifier {
    @Override
    public void modify(UserDataReplacer userData, String secretValue) {
        userData.replace("SALT_BOOT_PASSWORD", secretValue);
    }
}
