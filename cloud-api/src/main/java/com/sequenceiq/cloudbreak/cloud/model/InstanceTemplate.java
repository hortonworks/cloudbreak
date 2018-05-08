package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class InstanceTemplate extends DynamicModel {

    private final String flavor;

    private final String groupName;

    private final Long privateId;

    private final List<Volume> volumes;

    private final InstanceStatus status;

    private final Long templateId;

    public InstanceTemplate(String flavor, String groupName, Long privateId, List<Volume> volumes, InstanceStatus status, Map<String, Object> parameters,
            Long templateId) {
        super(parameters);
        this.flavor = flavor;
        this.templateId = templateId;
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

    public Long getTemplateId() {
        return templateId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InstanceTemplate that = (InstanceTemplate) o;
        return Objects.equals(flavor, that.flavor)
                && Objects.equals(groupName, that.groupName)
                && Objects.equals(privateId, that.privateId)
                && Objects.equals(volumes, that.volumes)
                && status == that.status
                && Objects.equals(templateId, that.templateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flavor, groupName, privateId, volumes, status, templateId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("InstanceTemplate{");
        sb.append("flavor='").append(flavor).append('\'');
        sb.append(", groupName='").append(groupName).append('\'');
        sb.append(", privateId=").append(privateId);
        sb.append(", volumes=").append(volumes);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
