package com.sequenceiq.cloudbreak.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static <T> T readValue(String content, Class<T> valueType) throws IOException {
        if (content == null) {
            return null;
        }
        return MAPPER.readValue(content, valueType);
    }

    public static String write(Object object) throws JsonProcessingException {
        if (object == null) {
            return null;
        }
        return MAPPER.writeValueAsString(object);
    }

}
