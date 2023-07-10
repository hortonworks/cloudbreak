package com.sequenceiq.cloudbreak.rotation.secret.userdata;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class UserDataRotationContext extends RotationContext {

    private final List<Pair<UserDataSecretModifier, String>> secretModifierMap;

    public UserDataRotationContext(String resourceCrn, List<Pair<UserDataSecretModifier, String>> secretModifierMap) {
        super(resourceCrn);
        this.secretModifierMap = secretModifierMap;
    }

    public List<Pair<UserDataSecretModifier, String>> getSecretModifierMap() {
        return secretModifierMap;
    }
}
