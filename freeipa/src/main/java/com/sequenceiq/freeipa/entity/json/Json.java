package com.sequenceiq.freeipa.entity.json;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import net.sf.json.JSONObject;

public class Json implements Serializable {

    private String value;

    public Json(String value) {
        this.value = value;
    }

    public Json(Object value) throws JsonProcessingException {
        this.value = JsonUtil.writeValueAsString(value);
    }

    private Json() {

    }

    public String getValue() {
        return value;
    }

    public <T> T get(Class<T> valueType) throws IOException {
        return JsonUtil.readValue(value, valueType);
    }

    public static Json silent(Object value) {
        Json json = new Json();
        json.value = JsonUtil.writeValueAsStringSilent(value);
        return json;
    }

    @JsonIgnore
    public Map<String, Object> getMap() {
        try {
            if (value == null) {
                return new HashMap<>();
            }
            return get(Map.class);
        } catch (IOException ignored) {
            return new HashMap<>();
        }
    }

    @JsonIgnore
    public <T> T getValue(String path) {
        String[] split = path.split("\\.");
        JSONObject jsonObject = JSONObject.fromObject(value);
        if (split.length == 1) {
            return (T) jsonObject.get(split[0]);
        }

        JSONObject object = jsonObject;
        for (int i = 0; i < split.length - 1; i++) {
            object = object.getJSONObject(split[i]);
        }
        return (T) object.get(split[split.length - 1]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || !Objects.equals(getClass(), o.getClass())) {
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
