package com.sequenceiq.cloudbreak.common.json;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TypedJsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypedJsonUtil.class);

    private static final ObjectMapper TYPED_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
            .enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)
            .activateDefaultTypingAsProperty(LaissezFaireSubTypeValidator.instance, DefaultTyping.JAVA_LANG_OBJECT, "@type")
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .addModule(new Hibernate6Module())
            .build();

    private TypedJsonUtil() {
    }

    public static <T> T readValue(String content, Class<T> valueType) throws IOException {
        return TYPED_MAPPER.readValue(content, valueType);
    }

    public static <T> T readValueUnchecked(String content, Class<T> valueType) {
        try {
            return TYPED_MAPPER.readValue(content, valueType);
        } catch (IOException e) {
            LOGGER.error("Failed to deserialize with Jackson: {}", content, e);
            throw new IllegalStateException("Cannot convert Json string to object.", e);
        }
    }

    public static String writeValueAsStringSilent(Object object) {
        if (object != null) {
            try {
                return TYPED_MAPPER.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                LOGGER.warn("JSON serialization went wrong in silent mode: {}", e.getMessage());
            }
        }
        return null;
    }

}
