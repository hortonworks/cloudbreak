package com.sequenceiq.cloudbreak.workspace.model;

import java.io.Serializable;

import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class CompactView implements Serializable, WorkspaceAwareResource {

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
