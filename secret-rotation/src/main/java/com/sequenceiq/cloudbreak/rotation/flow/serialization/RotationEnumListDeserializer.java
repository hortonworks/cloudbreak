package com.sequenceiq.cloudbreak.rotation.flow.serialization;

import static com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil.deserializeList;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;

public class RotationEnumListDeserializer extends JsonDeserializer<List<SerializableRotationEnum>> {

    @Override
    public List<SerializableRotationEnum> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        try {
            return deserializeList(text);
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of enum list.", text), e);
        }
    }
}
