package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "Blueprint")
public class ClusterDefinitionView extends CompactView {
    private String stackType;

    private String stackVersion;

    private int hostGroupCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

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

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.CLUSTER_DEFINITION;
    }
}
