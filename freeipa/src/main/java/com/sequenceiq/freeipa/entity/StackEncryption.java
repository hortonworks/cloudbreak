package com.sequenceiq.freeipa.entity;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
public class StackEncryption implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackencryption_generator")
    @SequenceGenerator(name = "stackencryption_generator", sequenceName = "stackencryption_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "stackId", unique = true, nullable = false)
    private Long stackId;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret encryptionKeyLuks = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret encryptionKeyCloudSecretManager = Secret.EMPTY;

    @Column(name = "accountid", nullable = false)
    private String accountId;

    public StackEncryption() {

    }

    public StackEncryption(Long stackId) {
        this.stackId = stackId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getEncryptionKeyLuks() {
        return getIfNotNull(encryptionKeyLuks, Secret::getRaw);
    }

    public void setEncryptionKeyLuks(String encryptionKeyLuks) {
        this.encryptionKeyLuks = new Secret(encryptionKeyLuks);
    }

    public String getEncryptionKeyCloudSecretManager() {
        return getIfNotNull(encryptionKeyCloudSecretManager, Secret::getRaw);
    }

    public void setEncryptionKeyCloudSecretManager(String encryptionKeyCloudSecretManager) {
        this.encryptionKeyCloudSecretManager = new Secret(encryptionKeyCloudSecretManager);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
