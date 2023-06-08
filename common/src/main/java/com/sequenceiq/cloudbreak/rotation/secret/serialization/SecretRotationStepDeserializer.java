package com.sequenceiq.cloudbreak.rotation.secret.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public class SecretRotationStepDeserializer extends JsonDeserializer<Enum<? extends SecretRotationStep>> {

    @Override
    public Enum<? extends SecretRotationStep> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return SecretRotationEnumSerializationUtil.deserialize(p.getText());
    }
}