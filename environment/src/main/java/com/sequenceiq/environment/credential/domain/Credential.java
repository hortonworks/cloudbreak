package com.sequenceiq.environment.credential.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.parameters.dao.converter.CredentialTypeConverter;

@Entity
@Table
public class Credential implements Serializable, AuthResource, AccountAwareResource {

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
    @SecretValue
    private Secret attributes = Secret.EMPTY;

    @Column
    private Boolean govCloud = Boolean.FALSE;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String creator;

    @Column(nullable = false)
    private String resourceCrn;

    @Column
    private String verificationStatusText;

    @Column
    private boolean verifyPermissions;

    @Convert(converter = CredentialTypeConverter.class)
    private CredentialType type;

    private Long created = System.currentTimeMillis();

    public Long getCreated() {
        return created;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
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

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getVerificationStatusText() {
        return verificationStatusText;
    }

    public void setVerificationStatusText(String verificationStatusText) {
        this.verificationStatusText = verificationStatusText;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public boolean isVerifyPermissions() {
        return verifyPermissions;
    }

    public void setVerifyPermissions(boolean verifyPermissions) {
        this.verifyPermissions = verifyPermissions;
    }

}