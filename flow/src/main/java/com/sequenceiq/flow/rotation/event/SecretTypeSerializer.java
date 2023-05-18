package com.sequenceiq.flow.rotation.event;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeSerializer extends JsonSerializer<SecretType> {

    public static final String CLASS_KEY = "clazz";

    public static final String VALUE_KEY = "value";

    @Override
    public void serialize(SecretType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeString(JsonUtil.writeValueAsString(Map.of(CLASS_KEY, value.getClass().getName(), VALUE_KEY, value)));
        } else {
            gen.writeNull();
        }
    }
}
