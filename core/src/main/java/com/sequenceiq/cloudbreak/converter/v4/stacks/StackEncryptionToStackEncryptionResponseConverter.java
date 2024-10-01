package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.common.api.encryption.response.StackEncryptionResponse;

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
