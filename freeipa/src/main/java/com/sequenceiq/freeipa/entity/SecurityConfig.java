package com.sequenceiq.freeipa.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;

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

    @Override
    public String toString() {
        return "SecurityConfig{" +
                "id=" + id +
                ", clientKeyVault=" + clientKeyVault +
                ", clientCertVault=" + clientCertVault +
                ", saltSecurityConfig=" + saltSecurityConfig +
                ", usePrivateIpToTls=" + usePrivateIpToTls +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
