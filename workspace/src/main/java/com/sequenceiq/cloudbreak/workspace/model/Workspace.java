package com.sequenceiq.cloudbreak.workspace.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.workspace.util.WorkspaceStatusConverter;

@Entity
public class Workspace implements TenantAwareResource, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "workspace_generator")
    @SequenceGenerator(name = "workspace_generator", sequenceName = "workspace_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    private Tenant tenant;

    @Column(nullable = false)
    @Convert(converter = WorkspaceStatusConverter.class)
    private WorkspaceStatus status = WorkspaceStatus.ACTIVE;

    private String resourceCrn;

    private Long deletionTimestamp = -1L;

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

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

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Workspace that = (Workspace) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Workspace.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .add("tenant=" + tenant)
                .add("status=" + status)
                .add("resourceCrn='" + resourceCrn + "'")
                .add("deletionTimestamp=" + deletionTimestamp)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
