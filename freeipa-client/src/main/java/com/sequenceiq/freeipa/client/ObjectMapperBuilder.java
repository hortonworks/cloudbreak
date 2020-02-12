package com.sequenceiq.freeipa.client;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperBuilder {

    private static ObjectMapper mapper;

    private ObjectMapperBuilder() {
    }

    public static ObjectMapper getObjectMapper() {
        if (mapper == null) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            objectMapper.setDateFormat(new SimpleDateFormat("yyyyMMddHHmmss"));
            mapper = objectMapper;
        }
        return mapper;
    }
}
