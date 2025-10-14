package com.sequenceiq.thunderhead.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.thunderhead.model.EntityType;

@Entity
public class ClassicCluster implements AccountIdAwareResource {

    @Id
    private String crn;

    private String accountId;

    private String name;

    private String datacenterName;

    private String url;

    private String pvcCpEnvironmentCrn;

    private String userName;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret password;

    private EntityType type;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public void setDatacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPvcCpEnvironmentCrn() {
        return pvcCpEnvironmentCrn;
    }

    public void setPvcCpEnvironmentCrn(String pvcCpEnvironmentCrn) {
        this.pvcCpEnvironmentCrn = pvcCpEnvironmentCrn;
    }

    public void setAuth(String auth) {
        String[] authParts = auth.split(":");
        userName = authParts[0];
        password = new Secret(authParts[1]);
    }

    public String getUserName() {
        return userName;
    }

    public Secret getPassword() {
        return password;
    }

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "ClassicCluster{" +
                "crn='" + crn + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", type=" + type +
                '}';
    }
}
