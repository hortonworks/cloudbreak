package com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformDisksV4Response implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.DISK_TYPES)
    private Map<String, Collection<String>> diskTypes;

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_DISKS)
    private Map<String, String> defaultDisks;

    @ApiModelProperty(ConnectorModelDescription.DISK_MAPPINGS)
    private Map<String, Map<String, String>> diskMappings;

    @ApiModelProperty(ConnectorModelDescription.DISK_DISPLAYNAMES)
    private Map<String, Map<String, String>> displayNames;

    public PlatformDisksV4Response() {
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
}
