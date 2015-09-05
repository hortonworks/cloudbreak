package com.sequenceiq.cloudbreak.controller.json;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.controller.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.domain.FileSystemType;
import com.wordnik.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class FileSystemBase {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.FileSystem.NAME, required = true)
    private String name;

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.FileSystem.TYPE, required = true)
    private FileSystemType type;

    @ApiModelProperty(value = ModelDescriptions.FileSystem.DEFAULT, required = true)
    private boolean defaultFs;

    @ApiModelProperty(value = ModelDescriptions.FileSystem.PROPERTIES, required = true)
    private Map<String,String> properties;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FileSystemType getType() {
        return type;
    }

    public void setType(FileSystemType type) {
        this.type = type;
    }

    public boolean isDefaultFs() {
        return defaultFs;
    }

    public void setDefaultFs(boolean defaultFs) {
        this.defaultFs = defaultFs;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
