package com.sequenceiq.freeipa.converter.encryption;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;
import com.sequenceiq.freeipa.entity.StackEncryption;

@Component
public class StackEncryptionToStackEncryptionResponseConverter implements Converter<StackEncryption, StackEncryptionResponse> {

    @Override
    public StackEncryptionResponse convert(StackEncryption source) {
        StackEncryptionResponse response = new StackEncryptionResponse();
        response.setEncryptionKeyLuks(source.getEncryptionKeyLuks());
        response.setEncryptionKeyCloudSecretManager(source.getEncryptionKeyCloudSecretManager());
        return response;
    }
}
