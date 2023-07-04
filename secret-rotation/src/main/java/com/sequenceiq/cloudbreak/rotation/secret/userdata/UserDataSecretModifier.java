package com.sequenceiq.cloudbreak.rotation.secret.userdata;

import com.sequenceiq.cloudbreak.util.UserDataReplacer;

public interface UserDataSecretModifier {

    void modify(UserDataReplacer userData, String secretValue);
}
