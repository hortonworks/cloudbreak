package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.AnonymizingBase64Serializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueprintDetails implements Serializable {
    private Long id;

    private String name;

    private String description;

    private String blueprintName;

    @JsonSerialize(using = AnonymizingBase64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String blueprintJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public String getBlueprintJson() {
        return blueprintJson;
    }

    public void setBlueprintJson(String blueprintJson) {
        this.blueprintJson = blueprintJson;
    }
}
