package com.sequenceiq.freeipa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.converter.SeLinuxConverter;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.common.model.SeLinux;

@Entity
public class SecurityConfig implements AccountIdAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret clientKeyVault = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret clientCertVault = Secret.EMPTY;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SaltSecurityConfig saltSecurityConfig;

    @Column(nullable = false)
    private boolean usePrivateIpToTls;

    private String accountId;

    @Convert(converter = SeLinuxConverter.class)
    @Column(name = "selinux")
    private SeLinux seLinux;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SaltSecurityConfig getSaltSecurityConfig() {
        return saltSecurityConfig;
    }

    public void setSaltSecurityConfig(SaltSecurityConfig saltSecurityConfig) {
        this.saltSecurityConfig = saltSecurityConfig;
    }

    public boolean isUsePrivateIpToTls() {
        return usePrivateIpToTls;
    }

    public void setUsePrivateIpToTls(boolean usePrivateIpToTls) {
        this.usePrivateIpToTls = usePrivateIpToTls;
    }

    @JsonProperty("clientKeyVault")
    public String getClientKeyVaultSecret() {
        return clientKeyVault.getSecret();
    }

    public String getClientKey() {
        return clientKeyVault.getRaw();
    }

    public void setClientKeyVault(Secret clientKeyVault) {
        this.clientKeyVault = clientKeyVault;
    }

    public void setClientKeyVault(String clientKeyVault) {
        this.clientKeyVault = new Secret(clientKeyVault);
    }

    @JsonProperty("clientCertVault")
    public String getClientCertVaultSecret() {
        return clientCertVault.getSecret();
    }

    public String getClientCert() {
        return clientCertVault.getRaw();
    }

    public void setClientCertVault(Secret clientCertVault) {
        this.clientCertVault = clientCertVault;
    }

    public void setClientCertVault(String clientCertVault) {
        this.clientCertVault = new Secret(clientCertVault);
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public SeLinux getSeLinux() {
        return seLinux;
    }

    public void setSeLinux(SeLinux seLinux) {
        this.seLinux = seLinux;
    }

    @Override
    public String toString() {
        return "SecurityConfig{" +
                "id=" + id +
                ", clientKeyVault=" + clientKeyVault +
                ", clientCertVault=" + clientCertVault +
                ", saltSecurityConfig=" + saltSecurityConfig +
                ", usePrivateIpToTls=" + usePrivateIpToTls +
                ", seLinux=" + seLinux +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
