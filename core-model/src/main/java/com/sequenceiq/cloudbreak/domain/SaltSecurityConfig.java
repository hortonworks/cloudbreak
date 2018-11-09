package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.aspect.secret.SecretValue;

@Entity
public class SaltSecurityConfig implements ProvisionEntity {

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

    public Secret getSaltBootSignPrivateKey() {
        return saltBootSignPrivateKey;
    }

    public void setSaltBootSignPrivateKey(String saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = new Secret(saltBootSignPrivateKey);
    }

    public void setSaltBootSignPrivateKey(Secret saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = saltBootSignPrivateKey;
    }

    public Secret getSaltPassword() {
        return saltPassword;
    }

    public void setSaltPassword(String saltPassword) {
        this.saltPassword = new Secret(saltPassword);
    }

    public void setSaltPassword(Secret saltPassword) {
        this.saltPassword = saltPassword;
    }

    public Secret getSaltBootPassword() {
        return saltBootPassword;
    }

    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = new Secret(saltBootPassword);
    }

    public void setSaltBootPassword(Secret saltBootPassword) {
        this.saltBootPassword = saltBootPassword;
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

    public Secret getSaltSignPrivateKey() {
        return saltSignPrivateKey;
    }
}
