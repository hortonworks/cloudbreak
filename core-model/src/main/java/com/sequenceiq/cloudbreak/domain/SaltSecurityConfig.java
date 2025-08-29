package com.sequenceiq.cloudbreak.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
public class SaltSecurityConfig implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "saltsecurityconfig_generator")
    @SequenceGenerator(name = "saltsecurityconfig_generator", sequenceName = "saltsecurityconfig_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltPassword = Secret.EMPTY;

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String saltSignPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltSignPrivateKey = Secret.EMPTY;

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String saltMasterPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltMasterPrivateKey = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltBootPassword = Secret.EMPTY;

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    @Column(columnDefinition = "TEXT")
    private String saltBootSignPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltBootSignPrivateKey = Secret.EMPTY;

    @OneToOne(mappedBy = "saltSecurityConfig")
    private SecurityConfig securityConfig;

    @ManyToOne
    private Workspace workspace;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String getName() {
        return "saltsecurityconfig-" + id;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public String getSaltBootSignPublicKey() {
        String saltBootSignPrivateKey = getSaltBootSignPrivateKey();
        return saltBootSignPrivateKey != null ? PkiUtil.calculatePemPublicKeyInBase64(saltBootSignPrivateKey) : null;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public void setSaltBootSignPublicKey(String saltBootSignPublicKey) {
        this.saltBootSignPublicKey = saltBootSignPublicKey;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public String getLegacySaltBootSignPublicKey() {
        return saltBootSignPublicKey;
    }

    public String getSaltBootSignPrivateKey() {
        return saltBootSignPrivateKey.getRaw();
    }

    public void setSaltBootSignPrivateKey(String saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = new Secret(saltBootSignPrivateKey);
    }

    public void setSaltBootSignPrivateKeySecret(Secret saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = saltBootSignPrivateKey;
    }

    public Secret getSaltBootSignPrivateKeySecret() {
        return saltBootSignPrivateKey;
    }

    public String getSaltPassword() {
        return saltPassword.getRaw();
    }

    public String getSaltPasswordSecret() {
        return saltPassword.getSecret();
    }

    public void setSaltPassword(String saltPassword) {
        this.saltPassword = new Secret(saltPassword);
    }

    public void setSaltPasswordSecret(Secret saltPassword) {
        this.saltPassword = saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword.getRaw();
    }

    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = new Secret(saltBootPassword);
    }

    public void setSaltBootPasswordSecret(Secret saltBootPassword) {
        this.saltBootPassword = saltBootPassword;
    }

    public Secret getSaltBootPasswordSecret() {
        return saltBootPassword;
    }

    public String getSaltMasterPrivateKey() {
        return saltMasterPrivateKey.getRaw();
    }

    public void setSaltMasterPrivateKey(String saltMasterPrivateKey) {
        this.saltMasterPrivateKey = new Secret(saltMasterPrivateKey);
    }

    public void setSaltMasterPrivateKeySecret(Secret saltMasterPrivateKey) {
        this.saltMasterPrivateKey = saltMasterPrivateKey;
    }

    public Secret getSaltMasterPrivateKeySecret() {
        return saltMasterPrivateKey;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public String getLegacySaltMasterPublicKey() {
        return saltMasterPublicKey;
    }

    public String getSaltMasterPublicKey() {
        String saltMasterPrivateKey = getSaltMasterPrivateKey();
        return saltMasterPrivateKey != null ? PkiUtil.calculatePemPublicKeyInBase64(saltMasterPrivateKey) : null;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public void setSaltMasterPublicKey(String saltMasterPublicKey) {
        this.saltMasterPublicKey = saltMasterPublicKey;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public String getLegacySaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public String getSaltSignPublicKey() {
        String saltSignPrivateKey = getSaltSignPrivateKey();
        return saltSignPrivateKey != null ? PkiUtil.calculatePemPublicKeyInBase64(saltSignPrivateKey) : null;
    }

    /**
     * @deprecated calculate public key based on the private key instead
     */
    @Deprecated
    public void setSaltSignPublicKey(String saltSignPublicKey) {
        this.saltSignPublicKey = saltSignPublicKey;
    }

    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey.getRaw();
    }

    public void setSaltSignPrivateKey(String saltSignPrivateKey) {
        this.saltSignPrivateKey = new Secret(saltSignPrivateKey);
    }

    public void setSaltSignPrivateKeySecret(Secret saltSignPrivateKey) {
        this.saltSignPrivateKey = saltSignPrivateKey;
    }

    public Secret getSaltSignPrivateKeySecret() {
        return saltSignPrivateKey;
    }

}
