package com.sequenceiq.cloudbreak.rotation.flow.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;

public class SecretTypeDeserializer extends JsonDeserializer<Enum<? extends SecretType>> {

    @Override
    public Enum<? extends SecretType> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return SecretRotationEnumSerializationUtil.deserialize(p.getText());
    }
}
