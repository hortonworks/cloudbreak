package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.AnonymizingBase64Serializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterDefinitionDetails implements Serializable {
    private Long id;

    private String name;

    private String description;

    private String clusterDefinitionName;

    @JsonSerialize(using = AnonymizingBase64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String clusterDefinitionJson;

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

    public String getClusterDefinitionName() {
        return clusterDefinitionName;
    }

    public void setClusterDefinitionName(String clusterDefinitionName) {
        this.clusterDefinitionName = clusterDefinitionName;
    }

    public String getClusterDefinitionJson() {
        return clusterDefinitionJson;
    }

    public void setClusterDefinitionJson(String clusterDefinitionJson) {
        this.clusterDefinitionJson = clusterDefinitionJson;
    }
}
