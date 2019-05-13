package com.sequenceiq.environment.credential;

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

import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;
import com.sequenceiq.secret.domain.Secret;
import com.sequenceiq.secret.domain.SecretToString;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Credential implements WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "credential_generator")
    @SequenceGenerator(name = "credential_generator", sequenceName = "credential_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "boolean default false")
    private boolean archived;

    @Column(nullable = false)
    private String cloudPlatform;

    @Convert(converter = SecretToString.class)
    private Secret attributes = Secret.EMPTY;

    @ManyToOne
    private Workspace workspace;

    @Column
    private Boolean govCloud = Boolean.FALSE;

    @Override
    public Long getId() {
        return null;
    }

    @Override
    public Workspace getWorkspace() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setWorkspace(Workspace workspace) {

    }

    @Override
    public WorkspaceResource getResource() {
        return null;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }
    public String getAttributes() {
        return attributes.getRaw();
    }

    public String getAttributesSecret() {
        return attributes.getSecret();
    }

    public void setAttributes(String attributes) {
        this.attributes = new Secret(attributes);
    }

    public void setAttributes(Secret attributes) {
        this.attributes = attributes;
    }

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }
}