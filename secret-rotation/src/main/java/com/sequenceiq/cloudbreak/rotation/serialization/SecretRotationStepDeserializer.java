package com.sequenceiq.cloudbreak.rotation.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;

public class SecretRotationStepDeserializer extends JsonDeserializer<Enum<? extends SecretRotationStep>> {

    @Override
    public Enum<? extends SecretRotationStep> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return SecretRotationEnumSerializationUtil.deserialize(p.getText());
    }
}