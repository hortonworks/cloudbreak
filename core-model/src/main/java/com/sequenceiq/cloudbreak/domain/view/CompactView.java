package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@MappedSuperclass
@Deprecated
public abstract class CompactView implements ProvisionEntity, WorkspaceAwareResource {

    @ManyToOne
    private Workspace workspace;

    @Id
    private Long id;

    private String name;

    private String description;

    public CompactView() {
    }

    public CompactView(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
