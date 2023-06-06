package com.sequenceiq.flow.rotation.serialization;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeListSerializer extends JsonSerializer<List<Enum<? extends SecretType>>> {

    @Override
    public void serialize(List<Enum<? extends SecretType>> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        SecretTypeSerializer.serialize(value, gen, () -> value.stream()
                .map(secretType -> new SecretTypeClassWrapper((Class<Enum<? extends SecretType>>) secretType.getClass(), secretType.name()))
                .collect(Collectors.toList()));
    }
}
