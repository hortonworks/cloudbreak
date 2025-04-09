package com.sequenceiq.cloudbreak.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.converter.BlueprintHybridOptionConverter;
import com.sequenceiq.cloudbreak.domain.converter.BlueprintUpgradeOptionConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
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
    @Convert(converter = BlueprintSecretToString.class)
    @SecretValue
    private Secret blueprintText = Secret.EMPTY;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String defaultBlueprintText;

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

    private String stackType;

    private String stackVersion;

    private Long created = System.currentTimeMillis();

    private Long lastUpdated = System.currentTimeMillis();

    @Convert(converter = BlueprintUpgradeOptionConverter.class)
    private BlueprintUpgradeOption blueprintUpgradeOption;

    @Convert(converter = BlueprintHybridOptionConverter.class)
    private BlueprintHybridOption hybridOption;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    /**
     * Default blueprints might have a null Vault path, after CB-24813, therefore this method might return null.
     * @deprecated use {@link #getBlueprintJsonText()} instead!
     */
    @Deprecated
    public String getBlueprintText() {
        return blueprintText.getRaw();
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = new Secret(blueprintText);
    }

    public void setBlueprintTextToBlankIfDefaultTextIsPresent(String blueprintText) {
        if (defaultBlueprintText != null) {
            this.blueprintText = Secret.EMPTY;
        } else {
            setBlueprintText(blueprintText);
        }
    }

    public String getBlueprintJsonText() {
        if (defaultBlueprintText != null) {
            return defaultBlueprintText;
        } else {
            return getBlueprintText();
        }
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

    public BlueprintHybridOption getHybridOption() {
        return hybridOption;
    }

    public void setHybridOption(BlueprintHybridOption hybridOption) {
        this.hybridOption = hybridOption;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getDefaultBlueprintText() {
        return defaultBlueprintText;
    }

    public void setDefaultBlueprintText(String defaultBlueprintText) {
        this.defaultBlueprintText = defaultBlueprintText;
    }

    @Override
    public String toString() {
        return "Blueprint{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", stackName='" + stackName + '\'' +
                ", description='" + description + '\'' +
                ", hostGroupCount=" + hostGroupCount +
                ", status=" + status +
                ", tags=" + tags +
                ", workspace=" + workspace +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", stackType='" + stackType + '\'' +
                ", stackVersion='" + stackVersion + '\'' +
                ", created=" + created +
                ", blueprintUpgradeOption=" + blueprintUpgradeOption +
                ", hybridOption=" + hybridOption +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
