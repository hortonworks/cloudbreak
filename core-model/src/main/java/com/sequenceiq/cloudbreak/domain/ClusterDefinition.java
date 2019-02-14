package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
@Table(name = "blueprint", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class ClusterDefinition implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blueprint_generator")
    @SequenceGenerator(name = "blueprint_generator", sequenceName = "blueprint_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "blueprintText", nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret clusterDefinitionText = Secret.EMPTY;

    @Column(name = "ambariName")
    private String stackName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private int hostGroupCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @ManyToOne
    private Workspace workspace;

    private String stackType;

    private String stackVersion;

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClusterDefinitionText() {
        return clusterDefinitionText.getRaw();
    }

    public void setClusterDefinitionText(String clusterDefinitionText) {
        this.clusterDefinitionText = new Secret(clusterDefinitionText);
    }

    public String getStackName() {
        return stackName;
    }

    public void setStackName(String stackName) {
        this.stackName = stackName;
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

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.CLUSTER_DEFINITION;
    }

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
}
