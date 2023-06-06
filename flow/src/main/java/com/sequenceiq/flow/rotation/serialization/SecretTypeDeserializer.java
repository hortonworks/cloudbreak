package com.sequenceiq.flow.rotation.serialization;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeDeserializer extends JsonDeserializer<Enum<? extends SecretType>> {

    @Override
    public Enum<? extends SecretType> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String secretTypeWithClass = p.getText();
        try {
            SecretTypeClassWrapper wrapper = JsonUtil.readValue(secretTypeWithClass, SecretTypeClassWrapper.class);
            return getSecretTypeFromWrapper(wrapper);
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of SecretType.", secretTypeWithClass), e);
        }
    }

    public static Enum<? extends SecretType> getSecretTypeFromWrapper(SecretTypeClassWrapper wrapper) {
        return Arrays.stream(wrapper.clazz().getEnumConstants())
                .filter(enumConstant -> StringUtils.equals(enumConstant.name(), wrapper.value()))
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(String.format("There is no SecretType enum for value %s", wrapper.value())));
    }
}
