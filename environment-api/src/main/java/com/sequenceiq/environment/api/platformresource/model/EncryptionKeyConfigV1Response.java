package com.sequenceiq.environment.api.platformresource.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EncryptionKeyConfigV1Response implements Serializable {

    private String name;

    private String id;

    private String description;

    private String displayName;

    private Map<String, Object> properties = new HashMap<>();

    public EncryptionKeyConfigV1Response() {

    }

    public EncryptionKeyConfigV1Response(String name, String id, String description, String displayName, Map<String, Object> properties) {
        this.name = name;
        this.id = id;
        this.properties = properties;
        this.description = description;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
