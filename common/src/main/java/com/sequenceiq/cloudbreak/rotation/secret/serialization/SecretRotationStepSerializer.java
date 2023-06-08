package com.sequenceiq.cloudbreak.rotation.secret.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

public class SecretRotationStepSerializer extends JsonSerializer<Enum<? extends SecretRotationStep>> {

    @Override
    public void serialize(Enum<? extends SecretRotationStep> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        SecretRotationEnumSerializationUtil.serialize(value, gen);
    }
}
