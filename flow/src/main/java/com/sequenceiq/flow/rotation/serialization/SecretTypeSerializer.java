package com.sequenceiq.flow.rotation.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.serialization.SecretRotationEnumSerializationUtil;

public class SecretTypeSerializer extends JsonSerializer<Enum<? extends SecretType>> {

    @Override
    public void serialize(Enum<? extends SecretType> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        SecretRotationEnumSerializationUtil.serialize(value, gen);
    }
}
