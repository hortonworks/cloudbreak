package com.sequenceiq.cloudbreak.common.json;

import static com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil.anonymize;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class Json implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Json.class);

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

    /**
     * Need this for Jackson deserialization
     * @param value JSON string
     */
    private void setValue(String value) {
        this.value = value;
    }

    public <T> T get(Class<T> valueType) throws IOException {
        return JsonUtil.readValue(value, valueType);
    }

    public <T> T getSilent(Class<T> valueType) {
        try {
            return JsonUtil.readValue(value, valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public <T> T get(TypeReference<T> valueType) throws IOException {
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
        JSONObject jsonObject = JSONObject.fromObject(value);
        if (jsonObject.isEmpty()) {
            return null;
        }
        String[] split = path.split("\\.");
        if (split.length == 1) {
            return (T) jsonObject.get(split[0]);
        }

        JSONObject object = jsonObject;
        for (int i = 0; i < split.length - 1; i++) {
            if (object.isEmpty()) {
                return null;
            }
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

        if (isObject() && json.isObject()) {
            return JSONObject.fromObject(value).equals(JSONObject.fromObject(json.value));
        } else if (isArray() && json.isArray()) {
            return JSONArray.fromObject(value).equals(JSONArray.fromObject(json.value));
        } else {
            return new EqualsBuilder()
                    .append(value, json.value)
                    .isEquals();
        }
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Json{");
        sb.append("value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public boolean isObject() {
        try {
            JSONObject.fromObject(value);
            return true;
        } catch (JSONException e) {
            LOGGER.trace("This json is not an Object: {}", e.getMessage());
            return false;
        }
    }

    public boolean isArray() {
        try {
            JSONArray.fromObject(value);
            return true;
        } catch (JSONException e) {
            LOGGER.trace("This json is not an Array: {}", anonymize(e.getMessage()));
            return false;
        }
    }

    public List<String> asArray() {
        return (List<String>) JSONArray.fromObject(value).stream().map(Object::toString).collect(Collectors.toList());
    }
}
