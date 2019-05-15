package com.sequenceiq.periscope.model.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class Json {

    private static final Logger LOGGER = LoggerFactory.getLogger(Json.class);

    private final String value;

    Json(String value) {
        this.value = value;
    }

    public Json(Object value) throws JsonProcessingException {
        this.value = JsonUtil.writeValueAsString(value);
    }

    public String getValue() {
        return value;
    }

    public <T> T get(Class<T> valueType) throws IOException {
        return JsonUtil.readValue(value, valueType);
    }

    public Map<String, Object> getMap() {
        try {
            if (value == null) {
                return Collections.emptyMap();
            }
            return get(Map.class);
        } catch (IOException e) {
            LOGGER.info("Failed to convert to map", e);
            return Collections.emptyMap();
        }
    }

}
