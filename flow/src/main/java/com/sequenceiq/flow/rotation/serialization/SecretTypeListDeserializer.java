package com.sequenceiq.flow.rotation.serialization;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeListDeserializer extends JsonDeserializer<List<Enum<? extends SecretType>>> {

    @Override
    public List<Enum<? extends SecretType>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String secretTypesWithClass = p.getText();
        try {
            List<SecretTypeClassWrapper> wrapperList = JsonUtil.readValue(secretTypesWithClass, List.class);
            return wrapperList.stream()
                    .map(SecretTypeDeserializer::getSecretTypeFromWrapper)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of SecretType.", secretTypesWithClass), e);
        }
    }
}
