package com.sequenceiq.caas.util;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.caas.exception.JsonOperationException;

@Component
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String toJsonString(Object o) {
        return jsonOperationWithExceptionChange(() -> MAPPER.writeValueAsString(o));
    }

    public  <T> T toObject(String json, Class<T> type) {
        return jsonOperationWithExceptionChange(() -> MAPPER.readValue(json, type));
    }

    private  <T> T jsonOperationWithExceptionChange(@Nonnull Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new JsonOperationException(e);
        }
    }

}
