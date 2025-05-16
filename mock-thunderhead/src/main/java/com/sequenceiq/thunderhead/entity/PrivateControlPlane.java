package com.sequenceiq.thunderhead.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.thunderhead.model.EntityType;

@Entity
public final class PrivateControlPlane implements AccountIdAwareResource {

    @Id
    private String crn;

    private String pvcTenantId;

    private String name;

    private String url;

    private String accessKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret secretKey;

    private EntityType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getPvcTenantId() {
        return pvcTenantId;
    }

    public void setPvcTenantId(String pvcTenantId) {
        this.pvcTenantId = pvcTenantId;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setAuth(String auth) {
        String[] authParts = auth.split(":");
        accessKey = authParts[0];
        secretKey = new Secret(authParts[1]);
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Secret getSecretKey() {
        return secretKey;
    }

    @Override
    public String getAccountId() {
        return Crn.safeFromString(crn).getAccountId();
    }

    @Override
    public String toString() {
        return "PrivateControlPlane{" +
            "crn='" + crn + '\'' +
            ", name='" + name + '\'' +
            ", pvcId='" + pvcTenantId + '\'' +
            ", url='" + url + '\'' +
            ", type=" + type +
            '}';
    }
}
