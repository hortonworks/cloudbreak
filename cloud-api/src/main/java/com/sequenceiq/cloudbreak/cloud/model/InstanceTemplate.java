package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class InstanceTemplate extends DynamicModel {

    private final String flavor;

    private final String groupName;

    private final Long privateId;

    private final List<Volume> volumes;

    private final InstanceStatus status;

    public InstanceTemplate(String flavor, String groupName, Long privateId, List<Volume> volumes, InstanceStatus status, Map<String, Object> parameters) {
        super(parameters);
        this.flavor = flavor;
        this.groupName = groupName;
        this.privateId = privateId;
        this.volumes = ImmutableList.copyOf(volumes);
        this.status = status;
    }

    public String getFlavor() {
        return flavor;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public String getVolumeType() {
        return volumes.get(0).getType();
    }

    public int getVolumeSize() {
        return volumes.get(0).getSize();
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getPrivateId() {
        return privateId;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InstanceTemplate{");
        sb.append("flavor='").append(flavor).append('\'');
        sb.append(", groupName='").append(groupName).append('\'');
        sb.append(", privateId=").append(privateId);
        sb.append(", volumes=").append(volumes);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
