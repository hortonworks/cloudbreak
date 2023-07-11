package com.sequenceiq.cloudbreak.rotation.flow.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;
import com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil;

public class RotationEnumDeserializer extends JsonDeserializer<SerializableRotationEnum> {

    @Override
    public SerializableRotationEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return SecretRotationEnumSerializationUtil.deserialize(p.getText());
    }
}