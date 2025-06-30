package com.sequenceiq.freeipa.entity;

import java.util.Objects;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
public class CrossRealmTrust implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "crossrealmtrust_generator")
    @SequenceGenerator(name = "crossrealmtrust_generator", sequenceName = "crossrealmtrust_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Stack stack;

    private String environmentCrn;

    private String fqdn;

    private String ip;

    private String realm;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret trustSecret = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getTrustSecret() {
        return trustSecret.getRaw();
    }

    public String getTrustSecretSecret() {
        return trustSecret.getSecret();
    }

    public Secret getTrustSecretSecretObject() {
        return trustSecret;
    }

    public void setTrustSecret(String trustSecret) {
        this.trustSecret = new Secret(trustSecret);
    }

    @Override
    public String getAccountId() {
        return Objects.requireNonNull(Crn.fromString(environmentCrn)).getAccountId();
    }

    @Override
    public String toString() {
        return "CrossRealmTrust{" +
                "id=" + id +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                ", realm='" + realm + '\'' +
                '}';
    }
}
