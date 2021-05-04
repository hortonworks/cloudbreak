package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.converter.BlueprintUpgradeOptionConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name", "resourceCrn"}))
public class Blueprint implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blueprint_generator")
    @SequenceGenerator(name = "blueprint_generator", sequenceName = "blueprint_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret blueprintText = Secret.EMPTY;

    private String stackName;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private int hostGroupCount;

    @Column(nullable = false)
    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    @ManyToOne
    private Workspace workspace;

    private String resourceCrn;

    private String creator;

    private String stackType;

    private String stackVersion;

    private Long created = System.currentTimeMillis();

    @Convert(converter = BlueprintUpgradeOptionConverter.class)
    private BlueprintUpgradeOption blueprintUpgradeOption;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

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

    public String getBlueprintText() {
        return blueprintText.getRaw();
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = new Secret(blueprintText);
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
}
