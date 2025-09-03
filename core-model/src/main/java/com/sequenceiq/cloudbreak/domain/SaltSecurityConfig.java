package com.sequenceiq.cloudbreak.domain;

import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_BOOT_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_BOOT_SIGN_PRIVATE_KEY;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_MASTER_PRIVATE_KEY;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_PASSWORD;
import static com.sequenceiq.cloudbreak.service.secret.SecretMarker.SALT_SIGN_PRIVATE_KEY;

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
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
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

    @SecretSetter(marker = SALT_BOOT_SIGN_PRIVATE_KEY)
    public void setSaltBootSignPrivateKeySecret(Secret saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = saltBootSignPrivateKey;
    }

    @SecretGetter(marker = SALT_BOOT_SIGN_PRIVATE_KEY)
    public Secret getSaltBootSignPrivateKeySecret() {
        return saltBootSignPrivateKey;
    }

    public String getSaltPassword() {
        return saltPassword.getRaw();
    }

    @SecretGetter(marker = SALT_PASSWORD)
    public String getSaltPasswordSecret() {
        return saltPassword.getSecret();
    }

    public void setSaltPassword(String saltPassword) {
        this.saltPassword = new Secret(saltPassword);
    }

    @SecretSetter(marker = SALT_PASSWORD)
    public void setSaltPasswordSecret(Secret saltPassword) {
        this.saltPassword = saltPassword;
    }

    public String getSaltBootPassword() {
        return saltBootPassword.getRaw();
    }

    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = new Secret(saltBootPassword);
    }

    @SecretSetter(marker = SALT_BOOT_PASSWORD)
    public void setSaltBootPasswordSecret(Secret saltBootPassword) {
        this.saltBootPassword = saltBootPassword;
    }

    @SecretGetter(marker = SALT_BOOT_PASSWORD)
    public Secret getSaltBootPasswordSecret() {
        return saltBootPassword;
    }

    public String getSaltMasterPrivateKey() {
        return saltMasterPrivateKey.getRaw();
    }

    public void setSaltMasterPrivateKey(String saltMasterPrivateKey) {
        this.saltMasterPrivateKey = new Secret(saltMasterPrivateKey);
    }

    @SecretSetter(marker = SALT_MASTER_PRIVATE_KEY)
    public void setSaltMasterPrivateKeySecret(Secret saltMasterPrivateKey) {
        this.saltMasterPrivateKey = saltMasterPrivateKey;
    }

    @SecretGetter(marker = SALT_MASTER_PRIVATE_KEY)
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

    @SecretSetter(marker = SALT_SIGN_PRIVATE_KEY)
    public void setSaltSignPrivateKeySecret(Secret saltSignPrivateKey) {
        this.saltSignPrivateKey = saltSignPrivateKey;
    }

    @SecretGetter(marker = SALT_SIGN_PRIVATE_KEY)
    public Secret getSaltSignPrivateKeySecret() {
        return saltSignPrivateKey;
    }

}
