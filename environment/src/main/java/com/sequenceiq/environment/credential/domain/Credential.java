package com.sequenceiq.environment.credential.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
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

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Column(nullable = false)
    private String creator;

    @Column(nullable = false)
    private String resourceCrn;

    @Column
    private String verificationStatusText;

    @Embedded
    private CredentialSettings credentialSettings;

    @Convert(converter = CredentialTypeConverter.class)
    private CredentialType type;

    private Long created = System.currentTimeMillis();

    public Long getCreated() {
        return created;
    }

    /**
     * Need this for Jackson deserialization
     * @param created timestamp
     */
    private void setCreated(Long created) {
        this.created = created;
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

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
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

    public CredentialSettings getCredentialSettings() {
        return credentialSettings;
    }

    public void setCredentialSettings(CredentialSettings credentialSettings) {
        this.credentialSettings = credentialSettings;
    }

    @Override
    public String toString() {
        return "Credential{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", archived=" + archived +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", govCloud=" + govCloud +
                ", accountId='" + accountId + '\'' +
                ", creator='" + creator + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", verificationStatusText='" + verificationStatusText + '\'' +
                ", credentialSettings=" + credentialSettings +
                ", type=" + type +
                ", created=" + created +
                '}';
    }
}
