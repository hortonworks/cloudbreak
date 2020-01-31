package com.sequenceiq.environment.api.v1.platformresource.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.v1.platformresource.PlatformResourceModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDisksResponse implements Serializable {

    @ApiModelProperty(PlatformResourceModelDescription.DISK_TYPES)
    private Map<String, Collection<String>> diskTypes;

    @ApiModelProperty(PlatformResourceModelDescription.DEFAULT_DISKS)
    private Map<String, String> defaultDisks;

    @ApiModelProperty(PlatformResourceModelDescription.DISK_MAPPINGS)
    private Map<String, Map<String, String>> diskMappings;

    @ApiModelProperty(PlatformResourceModelDescription.DISK_DISPLAYNAMES)
    private Map<String, Map<String, String>> displayNames;

    public PlatformDisksResponse() {
        diskTypes = new HashMap<>();
        defaultDisks = new HashMap<>();
        diskMappings = new HashMap<>();
        displayNames = new HashMap<>();
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

    public Map<String, Map<String, String>> getDisplayNames() {
        return displayNames;
    }

    public void setDisplayNames(Map<String, Map<String, String>> displayNames) {
        this.displayNames = displayNames;
    }

    @Override
    public String toString() {
        return "PlatformDisksResponse{" +
                "diskTypes=" + diskTypes +
                ", defaultDisks=" + defaultDisks +
                ", diskMappings=" + diskMappings +
                ", displayNames=" + displayNames +
                '}';
    }
}
