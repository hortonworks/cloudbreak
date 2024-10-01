package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;

class StackEncryptionToStackEncryptionResponseConverterTest {

    private final StackEncryptionToStackEncryptionResponseConverter underTest = new StackEncryptionToStackEncryptionResponseConverter();

    @Test
    void testConvert() {
        StackEncryption source = new StackEncryption();
        source.setEncryptionKeyLuks("encryptionKeyLuks");
        source.setEncryptionKeyCloudSecretManager("encryptionKeyCloudSecretManager");

        StackEncryptionResponse result = underTest.convert(source);

        assertEquals("encryptionKeyLuks", result.getEncryptionKeyLuks());
        assertEquals("encryptionKeyCloudSecretManager", result.getEncryptionKeyCloudSecretManager());
    }
}
