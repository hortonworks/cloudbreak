package com.sequenceiq.cloudbreak.domain.json;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class Json implements Serializable {

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
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Json json = (Json) o;

        return new EqualsBuilder()
                .append(value, json.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(value)
                .toHashCode();
    }
}
