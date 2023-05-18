package com.sequenceiq.flow.rotation.event;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeDeserializer<T> extends JsonDeserializer<Enum<? extends SecretType>> {

    @Override
    public Enum<? extends SecretType> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String secretTypeWithClass = p.getText();
        try {
            Map<String, String> map = JsonUtil.readValue(secretTypeWithClass, Map.class);
            String className = map.get(SecretTypeSerializer.CLASS_KEY);
            String enumValue = map.get(SecretTypeSerializer.VALUE_KEY);
            Class<Enum> enumClass = (Class<Enum>) Class.forName(className);
            return Enum.valueOf(enumClass, enumValue);
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of SecretType.", secretTypeWithClass), e);
        }
    }
}
