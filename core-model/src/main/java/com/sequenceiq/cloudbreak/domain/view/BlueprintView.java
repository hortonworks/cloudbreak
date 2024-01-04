package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.domain.converter.BlueprintUpgradeOptionConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;

@Entity
@Table(name = "Blueprint")
@Deprecated
public class BlueprintView extends CompactView {
    private String stackType;

    private String stackVersion;

    private int hostGroupCount;

    @Column(nullable = false)
    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    private String resourceCrn;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    private Long created;

    @Convert(converter = BlueprintUpgradeOptionConverter.class)
    private BlueprintUpgradeOption blueprintUpgradeOption;

    private Long lastUpdated;

    public String getStackType() {
        return stackType;
    }

    public void setStackType(String stackType) {
        this.stackType = stackType;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    public int getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(int hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public BlueprintUpgradeOption getBlueprintUpgradeOption() {
        return blueprintUpgradeOption;
    }

    public void setBlueprintUpgradeOption(BlueprintUpgradeOption blueprintUpgradeOption) {
        this.blueprintUpgradeOption = blueprintUpgradeOption;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "BlueprintView{" +
                "stackType='" + stackType + '\'' +
                ", stackVersion='" + stackVersion + '\'' +
                ", hostGroupCount=" + hostGroupCount +
                ", status=" + status +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", tags=" + tags +
                ", created=" + created +
                ", blueprintUpgradeOption=" + blueprintUpgradeOption +
                ", lastUpdated=" + lastUpdated +
                "} " + super.toString();
    }
}
