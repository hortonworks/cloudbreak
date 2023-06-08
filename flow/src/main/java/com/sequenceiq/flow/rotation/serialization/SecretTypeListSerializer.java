package com.sequenceiq.flow.rotation.serialization;

import static com.sequenceiq.cloudbreak.rotation.secret.serialization.SecretRotationEnumSerializationUtil.listToString;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.serialization.SecretRotationEnumSerializationUtil;

public class SecretTypeListSerializer extends JsonSerializer<List<Enum<? extends SecretType>>> {

    @Override
    public void serialize(List<Enum<? extends SecretType>> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            List<String> mapStringList = value.stream()
                    .map(SecretRotationEnumSerializationUtil::enumToMapString)
                    .collect(Collectors.toList());
            gen.writeString(listToString(mapStringList));
        } else {
            gen.writeNull();
        }
    }
}
