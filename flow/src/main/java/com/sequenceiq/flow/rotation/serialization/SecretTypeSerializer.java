package com.sequenceiq.flow.rotation.serialization;

import java.io.IOException;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeSerializer extends JsonSerializer<Enum<? extends SecretType>> {

    @Override
    public void serialize(Enum<? extends SecretType> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        serialize(value, gen, () -> new SecretTypeClassWrapper((Class<Enum<? extends SecretType>>) value.getClass(), value.name()));
    }

    public static void serialize(Object value, JsonGenerator gen, Supplier<Object> toJsonSupplier) throws IOException {
        if (value != null) {
            gen.writeString(JsonUtil.writeValueAsString(toJsonSupplier.get()));
        } else {
            gen.writeNull();
        }
    }
}
