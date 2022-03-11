package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
@Where(clause = "deleted = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Topology implements ProvisionEntity, WorkspaceAwareResource, ArchivableResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "topology_generator")
    @SequenceGenerator(name = "topology_generator", sequenceName = "topology_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String cloudPlatform;

    @Column(nullable = false)
    private boolean deleted;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<TopologyRecord> records = new ArrayList<>();

    @ManyToOne
    private Workspace workspace;

    private Long deletionTimestamp = -1L;

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

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public List<TopologyRecord> getRecords() {
        return records;
    }

    public void setRecords(List<TopologyRecord> records) {
        this.records = records;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    @Override
    public void setArchived(boolean archived) {
        this.deleted = archived;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {

    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    @Override
    public String toString() {
        return "Topology{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", deleted=" + deleted +
                ", deletionTimestamp=" + deletionTimestamp +
                '}';
    }
}
