package com.sequenceiq.cloudbreak.cloud;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

public interface CryptoConnector extends CloudPlatformAware {

    byte[] encrypt(byte[] input, EncryptionKeySource keySource, CloudCredential cloudCredential, String regionName, Map<String, String> encryptionContext);

    default String encrypt(EncryptRequest request) {
        byte[] ciphertextBinary = encrypt(request.input().getBytes(), request.keySource(), request.cloudCredential(),
                request.regionName(), request.encryptionContext());
        return Base64Util.encode(ciphertextBinary);
    }
}
