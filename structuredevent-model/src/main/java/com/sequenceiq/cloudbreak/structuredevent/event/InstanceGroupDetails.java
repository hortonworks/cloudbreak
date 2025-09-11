package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceGroupDetails implements Serializable {
    private String groupName;

    private String groupType;

    private Integer nodeCount;

    private String instanceType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<VolumeDetails> volumes = new ArrayList<>();

    private String temporaryStorage;

    private Integer rootVolumeSize;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> attributes = new HashMap<>();

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Long> runningInstances = new HashMap<>();

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public List<VolumeDetails> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeDetails> volumes) {
        this.volumes = volumes;
    }

    public String getTemporaryStorage() {
        return temporaryStorage;
    }

    public void setTemporaryStorage(String temporaryStorage) {
        this.temporaryStorage = temporaryStorage;
    }

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Long> getRunningInstances() {
        return runningInstances;
    }

    public void setRunningInstances(Map<String, Long> runningInstances) {
        this.runningInstances = runningInstances;
    }
}
