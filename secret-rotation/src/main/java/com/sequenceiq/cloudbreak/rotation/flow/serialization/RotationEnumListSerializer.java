package com.sequenceiq.cloudbreak.rotation.flow.serialization;

import static com.sequenceiq.cloudbreak.rotation.serialization.SecretRotationEnumSerializationUtil.serializeList;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;

public class RotationEnumListSerializer extends JsonSerializer<List<SerializableRotationEnum>> {

    @Override
    public void serialize(List<SerializableRotationEnum> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeString(serializeList(value));
        } else {
            gen.writeNull();
        }
    }
}
