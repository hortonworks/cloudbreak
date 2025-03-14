package com.sequenceiq.freeipa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
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

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltPasswordVault;

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String saltSignPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltSignPrivateKeyVault;

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String saltMasterPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltMasterPrivateKeyVault = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltBootPasswordVault;

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String saltBootSignPublicKey;

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

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public String getLegacySaltBootSignPublicKey() {
        return saltBootSignPublicKey;
    }

    public String getSaltBootSignPublicKey() {
        String saltBootSignPrivateKey = getSaltBootSignPrivateKeyVault();
        return saltBootSignPrivateKey != null ? PkiUtil.calculatePemPublicKeyInBase64(saltBootSignPrivateKey) : null;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public void setSaltBootSignPublicKey(String saltBootSignPublicKey) {
        this.saltBootSignPublicKey = saltBootSignPublicKey;
    }

    public String getSaltPasswordVault() {
        return saltPasswordVault.getRaw();
    }

    public void setSaltPasswordVault(Secret saltPasswordVault) {
        this.saltPasswordVault = saltPasswordVault;
    }

    public void setSaltPasswordVault(String saltPasswordVault) {
        this.saltPasswordVault = new Secret(saltPasswordVault);
    }

    public String getSaltPasswordVaultSecret() {
        return saltPasswordVault.getSecret();
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public String getLegacySaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public String getSaltSignPublicKey() {
        String saltSignPrivateKey = getSaltSignPrivateKeyVault();
        return saltSignPrivateKey != null ? PkiUtil.calculatePemPublicKeyInBase64(saltSignPrivateKey) : null;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public void setSaltSignPublicKey(String saltSignPublicKey) {
        this.saltSignPublicKey = saltSignPublicKey;
    }

    public String getSaltSignPrivateKeyVault() {
        return saltSignPrivateKeyVault.getRaw();
    }

    public void setSaltSignPrivateKeyVault(String saltSignPrivateKeyVault) {
        this.saltSignPrivateKeyVault = new Secret(saltSignPrivateKeyVault);
    }

    public void setSaltSignPrivateKeyVault(Secret saltSignPrivateKeyVault) {
        this.saltSignPrivateKeyVault = saltSignPrivateKeyVault;
    }

    public String getSaltSignPrivateKeyVaultSecret() {
        return saltSignPrivateKeyVault.getSecret();
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public String getLegacySaltMasterPublicKey() {
        return saltMasterPublicKey;
    }

    public String getSaltMasterPublicKey() {
        String saltMasterPrivateKey = getSaltMasterPrivateKeyVault();
        return saltMasterPrivateKey != null ? PkiUtil.calculatePemPublicKeyInBase64(saltMasterPrivateKey) : null;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public void setSaltMasterPublicKey(String saltMasterPublicKey) {
        this.saltMasterPublicKey = saltMasterPublicKey;
    }

    public String getSaltMasterPrivateKeyVault() {
        return saltMasterPrivateKeyVault.getRaw();
    }

    public void setSaltMasterPrivateKeyVault(Secret saltMasterPrivateKeyVault) {
        this.saltMasterPrivateKeyVault = saltMasterPrivateKeyVault;
    }

    public void setSaltMasterPrivateKeyVault(String saltMasterPrivateKeyVault) {
        this.saltMasterPrivateKeyVault = new Secret(saltMasterPrivateKeyVault);
    }

    public String getSaltMasterPrivateKeyVaultSecret() {
        return saltMasterPrivateKeyVault.getSecret();
    }

    public String getSaltBootPasswordVault() {
        return saltBootPasswordVault.getRaw();
    }

    public void setSaltBootPasswordVault(Secret saltBootPasswordVault) {
        this.saltBootPasswordVault = saltBootPasswordVault;
    }

    public void setSaltBootPasswordVault(String saltBootPasswordVault) {
        this.saltBootPasswordVault = new Secret(saltBootPasswordVault);
    }

    public String getSaltBootPasswordVaultSecret() {
        return saltBootPasswordVault.getSecret();
    }

    public String getSaltBootSignPrivateKeyVault() {
        return saltBootSignPrivateKeyVault.getRaw();
    }

    public void setSaltBootSignPrivateKeyVault(Secret saltBootSignPrivateKeyVault) {
        this.saltBootSignPrivateKeyVault = saltBootSignPrivateKeyVault;
    }

    public void setSaltBootSignPrivateKeyVault(String saltBootSignPrivateKeyVault) {
        this.saltBootSignPrivateKeyVault = new Secret(saltBootSignPrivateKeyVault);
    }

    public String getSaltBootSignPrivateKeyVaultSecret() {
        return saltBootSignPrivateKeyVault.getSecret();
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
