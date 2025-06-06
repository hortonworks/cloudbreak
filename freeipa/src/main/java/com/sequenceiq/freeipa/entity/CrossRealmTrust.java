package com.sequenceiq.freeipa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class CrossRealmTrust {

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

    @Override
    public String toString() {
        return "CrossRealmTrust{" +
                "id=" + id +
                ", stack=" + stack +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", fqdn='" + fqdn + '\'' +
                ", ip='" + ip + '\'' +
                ", realm='" + realm + '\'' +
                '}';
    }
}
