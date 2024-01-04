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

@Entity
public class SaltSecurityConfig implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "saltsecurityconfig_generator")
    @SequenceGenerator(name = "saltsecurityconfig_generator", sequenceName = "saltsecurityconfig_id_seq", allocationSize = 1)
    private Long id;

    private String saltPassword;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltPasswordVault;

    @Column(columnDefinition = "TEXT")
    private String saltSignPublicKey;

    @Column(columnDefinition = "TEXT")
    private String saltSignPrivateKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltSignPrivateKeyVault;

    private String saltBootPassword;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltBootPasswordVault;

    @Column(columnDefinition = "TEXT")
    private String saltBootSignPublicKey;

    @Column(columnDefinition = "TEXT")
    private String saltBootSignPrivateKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltBootSignPrivateKeyVault;

    @OneToOne(mappedBy = "saltSecurityConfig")
    private SecurityConfig securityConfig;

    private String accountId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public String getSaltBootSignPublicKey() {
        return saltBootSignPublicKey;
    }

    public void setSaltBootSignPublicKey(String saltBootSignPublicKey) {
        this.saltBootSignPublicKey = saltBootSignPublicKey;
    }

    @Deprecated
    public String getSaltPassword() {
        return saltPassword;
    }

    @Deprecated
    public void setSaltPassword(String saltPassword) {
        this.saltPassword = saltPassword;
    }

    public String getSaltPasswordVault() {
        return saltPasswordVault.getRaw();
    }

    public String getSaltPasswordVaultSecret() {
        return saltPasswordVault.getSecret();
    }

    public void setSaltPasswordVault(Secret saltPasswordVault) {
        this.saltPasswordVault = saltPasswordVault;
    }

    public void setSaltPasswordVault(String saltPasswordVault) {
        this.saltPasswordVault = new Secret(saltPasswordVault);
    }

    public String getSaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public void setSaltSignPublicKey(String saltSignPublicKey) {
        this.saltSignPublicKey = saltSignPublicKey;
    }

    @Deprecated
    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey;
    }

    @Deprecated
    public void setSaltSignPrivateKey(String saltSignPrivateKey) {
        this.saltSignPrivateKey = saltSignPrivateKey;
    }

    public String getSaltSignPrivateKeyVault() {
        return saltSignPrivateKeyVault.getRaw();
    }

    public String getSaltSignPrivateKeyVaultSecret() {
        return saltSignPrivateKeyVault.getSecret();
    }

    public void setSaltSignPrivateKeyVault(String saltSignPrivateKeyVault) {
        this.saltSignPrivateKeyVault = new Secret(saltSignPrivateKeyVault);
    }

    public void setSaltSignPrivateKeyVault(Secret saltSignPrivateKeyVault) {
        this.saltSignPrivateKeyVault = saltSignPrivateKeyVault;
    }

    @Deprecated
    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    @Deprecated
    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = saltBootPassword;
    }

    public String getSaltBootPasswordVault() {
        return saltBootPasswordVault.getRaw();
    }

    public String getSaltBootPasswordVaultSecret() {
        return saltBootPasswordVault.getSecret();
    }

    public void setSaltBootPasswordVault(Secret saltBootPasswordVault) {
        this.saltBootPasswordVault = saltBootPasswordVault;
    }

    public void setSaltBootPasswordVault(String saltBootPasswordVault) {
        this.saltBootPasswordVault = new Secret(saltBootPasswordVault);
    }

    @Deprecated
    public String getSaltBootSignPrivateKey() {
        return saltBootSignPrivateKey;
    }

    @Deprecated
    public void setSaltBootSignPrivateKey(String saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = saltBootSignPrivateKey;
    }

    public String getSaltBootSignPrivateKeyVault() {
        return saltBootSignPrivateKeyVault.getRaw();
    }

    public String getSaltBootSignPrivateKeyVaultSecret() {
        return saltBootSignPrivateKeyVault.getSecret();
    }

    public void setSaltBootSignPrivateKeyVault(Secret saltBootSignPrivateKeyVault) {
        this.saltBootSignPrivateKeyVault = saltBootSignPrivateKeyVault;
    }

    public void setSaltBootSignPrivateKeyVault(String saltBootSignPrivateKeyVault) {
        this.saltBootSignPrivateKeyVault = new Secret(saltBootSignPrivateKeyVault);
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
