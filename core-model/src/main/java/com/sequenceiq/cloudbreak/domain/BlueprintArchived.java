package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
@Table(name = "blueprint")
@Where(clause = "archived = true")
public class BlueprintArchived implements WorkspaceAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blueprint_generator")
    @SequenceGenerator(name = "blueprint_generator", sequenceName = "blueprint_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @ManyToOne
    private Workspace workspace;

    private boolean archived;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.BLUEPRINT;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
