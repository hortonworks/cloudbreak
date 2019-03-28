package com.sequenceiq.cloudbreak.domain.workspace;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
public class Workspace implements ProvisionEntity, TenantAwareResource {

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
    @Enumerated(EnumType.STRING)
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

    public void setResourceCrnByUser(User user) {
        if (user.getUserCrn() != null) {
            resourceCrn = Crn.builder()
                        .setAccountId(Crn.fromString(user.getUserCrn()).getAccountId())
                        .setResource(getTenant().getName() + "#" + getName())
                        .setResourceType(Crn.ResourceType.WORKSPACE)
                        .setService(Crn.fromString(user.getUserCrn()).getService())
                        .build()
                        .toString();
        }
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
    public int hashCode() {
        return Objects.hash(id);
    }
}
