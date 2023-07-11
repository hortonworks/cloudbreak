package com.sequenceiq.cloudbreak.rotation.flow.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;

public class RotationEnumSerializer extends JsonSerializer<SerializableRotationEnum> {

    @Override
    public void serialize(SerializableRotationEnum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        SecretRotationEnumSerializationUtil.serialize(value, gen);
    }
}
