package com.sequenceiq.cloudbreak.orchestrator.salt.rotation;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.rotation.userdata.UserDataSecretModifier;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;

@Component
public class SaltBootSignKeyUserDataModifier implements UserDataSecretModifier {
    @Override
    public void modify(UserDataReplacer userData, String secretValue) {
        userData.replace("SALT_BOOT_SIGN_KEY", BaseEncoding.base64().encode(PkiUtil.getPublicKeyDer(new String(Base64.decodeBase64(secretValue)))));
    }
}
