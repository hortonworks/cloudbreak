package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

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

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return "saltsecurityconfig-" + id;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
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

    public String getSaltBootSignPrivateKey() {
        return saltBootSignPrivateKey.getRaw();
    }

    public void setSaltBootSignPrivateKey(String saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = new Secret(saltBootSignPrivateKey);
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

    public void setSaltSignPublicKey(String saltSignPublicKey) {
        this.saltSignPublicKey = saltSignPublicKey;
    }

    public void setSaltSignPrivateKey(String saltSignPrivateKey) {
        this.saltSignPrivateKey = new Secret(saltSignPrivateKey);
    }

    public void setSaltSignPrivateKey(Secret saltSignPrivateKey) {
        this.saltSignPrivateKey = saltSignPrivateKey;
    }

    public String getSaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey.getRaw();
    }

    @Override
    public String toString() {
        return "SaltSecurityConfig{" +
                "id=" + id +
                '}';
    }
}
