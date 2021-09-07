package com.sequenceiq.environment.parameters.dao.domain;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
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

    public String getEncryptionKeySecret() {
        return getIfNotNull(encryptionKey, Secret::getSecret);
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = new Secret(encryptionKey);
    }
}