package com.sequenceiq.cloudbreak.domain;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.TemporaryStorageConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Template implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "template_generator")
    @SequenceGenerator(name = "template_generator", sequenceName = "template_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    private String instanceType;

    private Integer rootVolumeSize;

    private boolean deleted;

    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    private String cloudPlatform;

    @ManyToOne
    private Topology topology;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret secretAttributes = Secret.EMPTY;

    @ManyToOne
    private Workspace workspace;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<VolumeTemplate> volumeTemplates;

    @Convert(converter = TemporaryStorageConverter.class)
    private TemporaryStorage temporaryStorage = TemporaryStorage.ATTACHED_VOLUMES;

    private Integer instanceStorageCount;

    public Template() {
        deleted = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<VolumeTemplate> getVolumeTemplates() {
        return volumeTemplates;
    }

    public void setVolumeTemplates(Set<VolumeTemplate> volumeTemplates) {
        this.volumeTemplates = volumeTemplates;
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

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    public void setRootVolumeSize(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public String getSecretAttributes() {
        return secretAttributes.getRaw();
    }

    public void setSecretAttributes(String secretAttributes) {
        this.secretAttributes = new Secret(secretAttributes);
    }

    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setTemporaryStorage(TemporaryStorage temporaryStorage) {
        this.temporaryStorage = temporaryStorage;
    }

    public TemporaryStorage getTemporaryStorage() {
        return temporaryStorage;
    }

    public Integer getInstanceStorageCount() {
        return instanceStorageCount;
    }

    public void setInstanceStorageCount(Integer instanceStorageCount) {
        this.instanceStorageCount = instanceStorageCount;
    }

    @Override
    public String toString() {
        return "Template{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", instanceType='" + instanceType + '\'' +
                ", rootVolumeSize=" + rootVolumeSize +
                ", deleted=" + deleted +
                ", status=" + status +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", topology=" + topology +
                ", instanceStorageCount=" + instanceStorageCount +
                '}';
    }
}
