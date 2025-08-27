package com.sequenceiq.freeipa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.freeipa.dto.SidGeneration;
import com.sequenceiq.freeipa.entity.util.SidGenerationConverter;

@Entity
public class FreeIpa implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "freeipa_generator")
    @SequenceGenerator(name = "freeipa_generator", sequenceName = "freeipa_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Stack stack;

    private String hostname;

    private String domain;

    @Column(name = "admin_group_name")
    private String adminGroupName;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret adminPassword = Secret.EMPTY;

    @Convert(converter = SidGenerationConverter.class)
    private SidGeneration sidGeneration;

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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getAdminGroupName() {
        return adminGroupName;
    }

    public void setAdminGroupName(String adminGroupName) {
        this.adminGroupName = adminGroupName;
    }

    public Secret getAdminPasswordSecret() {
        return adminPassword;
    }

    public String getAdminPassword() {
        return adminPassword.getRaw();
    }

    public void setAdminPassword(Secret adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = new Secret(adminPassword);
    }

    public SidGeneration getSidGeneration() {
        return sidGeneration;
    }

    public void setSidGeneration(SidGeneration sidGeneration) {
        this.sidGeneration = sidGeneration;
    }

    @Override
    public String getAccountId() {
        return stack.getAccountId();
    }

    public void evictVaultCacheForSecrets() {
        adminPassword.cacheEvict();
    }

    @Override
    public String toString() {
        return "FreeIpa{" +
                "id=" + id +
                ", stack=" + stack +
                ", hostname='" + hostname + '\'' +
                ", domain='" + domain + '\'' +
                ", adminGroupName='" + adminGroupName + '\'' +
                ", sidGeneration=" + sidGeneration +
                '}';
    }
}
