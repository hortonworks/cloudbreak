package com.sequenceiq.cloudbreak.common.json;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Json implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Json.class);

    private static final String SEGMENT_CHARACTER = ".";

    private static final String ESCAPED_SEGMENT_CHARACTER = "\\.";

    private String value;

    private Json() {
    }

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

    public static Json silent(Object value) {
        Json json = new Json();
        json.value = JsonUtil.writeValueAsStringSilent(value);
        return json;
    }

    public String getValue() {
        return value;
    }

    /**
     * Need this for Jackson deserialization
     *
     * @param value JSON string
     */
    private void setValue(String value) {
        this.value = value;
    }

    @JsonIgnore
    public <T> T get(Class<T> valueType) throws IOException {
        return JsonUtil.readValue(value, valueType);
    }

    @JsonIgnore
    public <T> T getUnchecked(Class<T> valueType) {
        try {
            return JsonUtil.readValue(value, valueType);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @JsonIgnore
    public <T> T get(String path, Class<T> valueType) {
        if (value == null) {
            LOGGER.warn("Json's value is null, cannot get object of type {} at path {}", valueType, path);
            return null;
        }
        Optional<JsonNode> jsonNode = traversePath(path);
        if (jsonNode.isPresent()) {
            return JsonUtil.readValue(jsonNode.get(), valueType);
        } else {
            LOGGER.warn("Could not find object of type {} at path {}", valueType, path);
            return null;
        }
    }

    @JsonIgnore
    public String getString(String path) {
        if (value == null) {
            LOGGER.warn("Json's value is null, cannot get String at path {}", path);
            return null;
        }
        Optional<JsonNode> jsonNode = traversePath(path);
        if (jsonNode.isPresent()) {
            return jsonNode.get().asText();
        } else {
            LOGGER.warn("Could not find String at path {}", path);
            return null;
        }
    }

    @JsonIgnore
    public Integer getInt(String path) {
        if (value == null) {
            LOGGER.warn("Json's value is null, cannot get Integer at path {}", path);
            return null;
        }
        Optional<JsonNode> jsonNode = traversePath(path);
        if (jsonNode.isPresent()) {
            return jsonNode.get().asInt();
        } else {
            LOGGER.warn("Could not find Integer at path {}", path);
            return null;
        }
    }

    @JsonIgnore
    public Double getDouble(String path) {
        if (value == null) {
            LOGGER.warn("Json's value is null, cannot get Double at path {}", path);
            return null;
        }
        Optional<JsonNode> jsonNode = traversePath(path);
        if (jsonNode.isPresent()) {
            return jsonNode.get().asDouble();
        } else {
            LOGGER.warn("Could not find Double at path {}", path);
            return null;
        }
    }

    @JsonIgnore
    public Boolean getBoolean(String path) {
        if (value == null) {
            LOGGER.warn("Json's value is null, cannot get Boolean at path {}", path);
            return null;
        }
        Optional<JsonNode> jsonNode = traversePath(path);
        if (jsonNode.isPresent()) {
            return jsonNode.get().asBoolean();
        } else {
            LOGGER.warn("Could not find Boolean at path {}", path);
            return null;
        }
    }

    @JsonIgnore
    public JsonNode getJsonNode(String path) {
        if (value == null) {
            LOGGER.warn("Json's value is null, cannot get JsonNode at path {}", path);
            return null;
        }
        Optional<JsonNode> jsonNode = traversePath(path);
        if (jsonNode.isPresent()) {
            return jsonNode.get();
        } else {
            LOGGER.warn("Could not find JsonNode at path {}", path);
            return null;
        }
    }

    @JsonIgnore
    public Map<String, Object> getMap() {
        if (value == null) {
            LOGGER.warn("Json's value is null, returning empty map");
            return new HashMap<>();
        }
        try {
            return get(Map.class);
        } catch (IOException e) {
            LOGGER.warn("Failed to parse Json value as Map, returning empty map", e);
            return new HashMap<>();
        }
    }

    @JsonIgnore
    public Set<String> flatPaths() {
        Set<String> accumulator = new HashSet<>();
        generateNode(getMap(), "", accumulator);
        return accumulator;
    }

    @JsonIgnore
    public void remove(String path) throws CloudbreakJsonProcessingException {
        JsonNode root = readTree();
        String[] split = path.split(ESCAPED_SEGMENT_CHARACTER);

        Optional<ObjectNode> objectNodeOpt = Optional.empty();
        if (split.length == 1) {
            objectNodeOpt = Optional.ofNullable((ObjectNode) root);
        } else {
            Optional<JsonNode> jsonNode = traversePathUntilLastParent(root, path);
            objectNodeOpt = jsonNode.filter(JsonNode::isObject).map(ObjectNode.class::cast);
        }

        objectNodeOpt.ifPresentOrElse(objectNode -> {
            objectNode.remove(split[split.length - 1]);
            this.value = root.toString();
        }, () -> LOGGER.info("Could not find parent node of path {}, nothing to remove", path));
    }

    @JsonIgnore
    public void replaceValue(String path, Object newValue) throws CloudbreakJsonProcessingException {
        JsonNode root = readTree();
        List<String> split = Arrays.asList(path.split(ESCAPED_SEGMENT_CHARACTER));
        Optional<JsonNode> jsonNodeOpt = traversePathUntilLastParent(root, path);
        jsonNodeOpt.map(ObjectNode.class::cast)
                .ifPresentOrElse(objectNode -> {
                    if (objectNode.has(split.getLast())) {
                        objectNode.replace(split.getLast(), JsonUtil.convertToTree(newValue));
                        this.value = root.toString();
                    }
                }, () -> LOGGER.info("Could not find parent node of path {}, nothing to replace", path));
    }

    @JsonIgnore
    public boolean isObject() {
        try {
            JsonNode jsonNode = readTree();
            return jsonNode.isObject();
        } catch (CloudbreakJsonProcessingException e) {
            LOGGER.info("Json's value is not a valid JSON", e);
            return false;
        }
    }

    @JsonIgnore
    public boolean isArray() {
        try {
            JsonNode jsonNode = readTree();
            return jsonNode.isArray();
        } catch (CloudbreakJsonProcessingException e) {
            LOGGER.info("Json's value is not a valid JSON", e);
            return false;
        }
    }

    private JsonNode readTree() throws CloudbreakJsonProcessingException {
        try {
            return JsonUtil.readTree(value);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new CloudbreakJsonProcessingException(e);
        }
    }

    private Optional<JsonNode> traversePath(String path) {
        try {
            JsonNode root = readTree();
            List<String> split = Arrays.asList(path.split(ESCAPED_SEGMENT_CHARACTER));
            return traversePath(root, split);
        } catch (CloudbreakJsonProcessingException e) {
            LOGGER.info("Json's value is not a valid JSON", e);
            return Optional.empty();
        }
    }

    private Optional<JsonNode> traversePath(JsonNode root, String path) {
        List<String> split = Arrays.asList(path.split(ESCAPED_SEGMENT_CHARACTER));
        return traversePath(root, split);
    }

    private Optional<JsonNode> traversePath(JsonNode root, List<String> split) {
        if (root.isEmpty() || root.isNull()) {
            return Optional.empty();
        }
        JsonNode currentNode = root;
        for (String key : split) {
            if (currentNode == null || currentNode.isEmpty()) {
                return Optional.empty();
            }
            currentNode = currentNode.get(key);
        }
        return Optional.ofNullable(currentNode);
    }

    private Optional<JsonNode> traversePathUntilLastParent(JsonNode root, String path) {
        List<String> split = Arrays.asList(path.split(ESCAPED_SEGMENT_CHARACTER));
        Optional<JsonNode> jsonNodeOpt = Optional.empty();
        if (split.size() > 1) {
            jsonNodeOpt = traversePath(root, split.subList(0, split.size() - 1).stream().collect(Collectors.joining(SEGMENT_CHARACTER)));
        } else {
            jsonNodeOpt = traversePath(root, List.of());
        }
        return jsonNodeOpt;
    }

    private void generateNode(Map<String, Object> map, String path, Set<String> accumulator) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            if (value instanceof Map) {
                generateNode((Map<String, Object>) value, path + key + '.', accumulator);
            } else if (value != null) {
                accumulator.add(path + key);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Json)) {
            return false;
        } else {
            try {
                JsonNode thisJson = readTree();
                JsonNode thatJson = JsonUtil.readTree(((Json) o).value);
                return thisJson.equals(thatJson);
            } catch (CloudbreakJsonProcessingException | JsonProcessingException | IllegalArgumentException e) {
                LOGGER.warn("At least one of the Json's value is not a valid JSON, falling back to string comparison", e);
                return Objects.equals(value, ((Json) o).value);
            }
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(value)
                .toHashCode();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Json{");
        sb.append("value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
