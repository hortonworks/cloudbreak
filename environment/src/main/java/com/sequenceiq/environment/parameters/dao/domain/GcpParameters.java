package com.sequenceiq.environment.parameters.dao.domain;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
@DiscriminatorValue("GCP")
public class GcpParameters extends BaseParameters implements AccountIdAwareResource {

    @Column(name = "encryption_key")
    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret encryptionKey = Secret.EMPTY;

    public String getEncryptionKey() {
        return getIfNotNull(encryptionKey, Secret::getRaw);
    }

    @JsonIgnore
    public String getEncryptionKeySecret() {
        return getIfNotNull(encryptionKey, Secret::getSecret);
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = new Secret(encryptionKey);
    }
}
