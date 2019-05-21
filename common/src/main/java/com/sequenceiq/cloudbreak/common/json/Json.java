package com.sequenceiq.cloudbreak.common.json;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    public Json(Object value) {
        try {
            this.value = JsonUtil.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
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
        return object.containsKey(split[split.length - 1]) ? (T) object.get(split[split.length - 1]) : null;
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

    @JsonIgnore
    public void remove(String path) {
        String[] split = path.split("\\.");
        JSONObject jsonObject = JSONObject.fromObject(value);
        if (split.length == 1) {
            jsonObject.remove(split[0]);
        }

        JSONObject object = jsonObject;
        for (int i = 0; i < split.length - 1; i++) {
            object = object.getJSONObject(split[i]);
        }
        object.remove(split[split.length - 1]);
        value = jsonObject.toString();
    }

    @JsonIgnore
    public Set<String> flatPaths() {
        Set<String> set = new HashSet<>();
        generateNode(getMap(), "", set);
        return set;
    }

    @JsonIgnore
    private void generateNode(Map map, String path, Set<String> set) {
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                generateNode((Map) value, path + key + '.', set);
            } else if (value != null) {
                set.add(path + key);
            }
        });
    }

    public void replaceValue(String path, String newValue) {
        String[] split = path.split("\\.");
        JSONObject jsonObject = JSONObject.fromObject(value);
        if (split.length == 1) {
            jsonObject.put(split[0], newValue);
        }

        JSONObject object = jsonObject;
        for (int i = 0; i < split.length - 1; i++) {
            object = object.getJSONObject(split[i]);
        }
        object.put(split[split.length - 1], newValue);
        value = jsonObject.toString();
    }
}
