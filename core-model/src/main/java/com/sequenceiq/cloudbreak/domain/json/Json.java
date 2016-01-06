package com.sequenceiq.cloudbreak.domain.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class Json {

    private String value;

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
            return Collections.emptyMap();
        }
    }

}
