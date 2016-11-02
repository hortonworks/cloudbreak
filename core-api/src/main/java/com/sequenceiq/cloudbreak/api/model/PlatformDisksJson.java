package com.sequenceiq.cloudbreak.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDisksJson implements JsonEntity {

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DISK_TYPES)
    private Map<String, Collection<String>> diskTypes;

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DEFAULT_DISKS)
    private Map<String, String> defaultDisks;

    @ApiModelProperty(ModelDescriptions.ConnectorModelDescription.DISK_MAPPINGS)
    private Map<String, Map<String, String>> diskMappings;

    public PlatformDisksJson() {
        this.diskTypes = new HashMap<>();
        this.defaultDisks = new HashMap<>();
        this.diskMappings = new HashMap<>();
    }

    public Map<String, Collection<String>> getDiskTypes() {
        return diskTypes;
    }

    public void setDiskTypes(Map<String, Collection<String>> diskTypes) {
        this.diskTypes = diskTypes;
    }

    public Map<String, String> getDefaultDisks() {
        return defaultDisks;
    }

    public void setDefaultDisks(Map<String, String> defaultDisks) {
        this.defaultDisks = defaultDisks;
    }

    public Map<String, Map<String, String>> getDiskMappings() {
        return diskMappings;
    }

    public void setDiskMappings(Map<String, Map<String, String>> diskMappings) {
        this.diskMappings = diskMappings;
    }
}
