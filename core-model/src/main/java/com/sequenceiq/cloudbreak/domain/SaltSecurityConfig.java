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

    @Column(columnDefinition = "TEXT")
    private String saltSignPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltSignPrivateKey = Secret.EMPTY;

    @Column(columnDefinition = "TEXT")
    private String saltMasterPublicKey;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltMasterPrivateKey = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret saltBootPassword = Secret.EMPTY;

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
        return saltBootSignPublicKey;
    }

    public void setSaltBootSignPublicKey(String saltBootSignPublicKey) {
        this.saltBootSignPublicKey = saltBootSignPublicKey;
    }

    public String getSaltBootSignPrivateKey() {
        return saltBootSignPrivateKey.getRaw();
    }

    public void setSaltBootSignPrivateKey(String saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = new Secret(saltBootSignPrivateKey);
    }

    public Secret getSaltBootSignPrivateKeySecret() {
        return saltBootSignPrivateKey;
    }

    public String getSaltPassword() {
        return saltPassword.getRaw();
    }

    public void setSaltPassword(String saltPassword) {
        this.saltPassword = new Secret(saltPassword);
    }

    public String getSaltBootPassword() {
        return saltBootPassword.getRaw();
    }

    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = new Secret(saltBootPassword);
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

    public String getSaltMasterPublicKey() {
        return saltMasterPublicKey;
    }

    public void setSaltMasterPublicKey(String saltMasterPublicKey) {
        this.saltMasterPublicKey = saltMasterPublicKey;
    }

    public String getSaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public void setSaltSignPublicKey(String saltSignPublicKey) {
        this.saltSignPublicKey = saltSignPublicKey;
    }

    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey.getRaw();
    }

    public void setSaltSignPrivateKey(String saltSignPrivateKey) {
        this.saltSignPrivateKey = new Secret(saltSignPrivateKey);
    }

}
