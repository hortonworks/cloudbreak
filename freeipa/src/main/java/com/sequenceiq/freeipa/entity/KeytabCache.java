package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

@Entity
public class KeytabCache implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "keytabcache_generator")
    @SequenceGenerator(name = "keytabcache_generator", sequenceName = "keytabcache_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String environmentCrn;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String principalHash;

    @Column(nullable = false)
    private String hostName;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret keytab = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret principal = Secret.EMPTY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPrincipalHash() {
        return principalHash;
    }

    public void setPrincipalHash(String principalHash) {
        this.principalHash = principalHash;
    }

    public Secret getKeytab() {
        return keytab;
    }

    public void setKeytab(String keytab) {
        this.keytab = new Secret(keytab);
    }

    public Secret getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = new Secret(principal);
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public String toString() {
        return "KeytabCache{" +
                "id=" + id +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", accountId='" + accountId + '\'' +
                ", principalHash='" + principalHash + '\'' +
                ", hostname='" + hostName + '\'' +
                ", keytab=" + keytab +
                ", principal=" + principal +
                '}';
    }
}
