package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
public class SaltSecurityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "saltsecurityconfig_generator")
    @SequenceGenerator(name = "saltsecurityconfig_generator", sequenceName = "saltsecurityconfig_id_seq", allocationSize = 1)
    private Long id;

    private String saltPassword;

    @Column(columnDefinition = "TEXT")
    private String saltSignPublicKey;

    @Column(columnDefinition = "TEXT")
    private String saltSignPrivateKey;

    private String saltBootPassword;

    @Column(columnDefinition = "TEXT")
    private String saltBootSignPublicKey;

    @Column(columnDefinition = "TEXT")
    private String saltBootSignPrivateKey;

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

    public String getSaltPassword() {
        return saltPassword;
    }

    public void setSaltPassword(String saltPassword) {
        this.saltPassword = saltPassword;
    }

    public String getSaltSignPublicKey() {
        return saltSignPublicKey;
    }

    public void setSaltSignPublicKey(String saltSignPublicKey) {
        this.saltSignPublicKey = saltSignPublicKey;
    }

    public String getSaltSignPrivateKey() {
        return saltSignPrivateKey;
    }

    public void setSaltSignPrivateKey(String saltSignPrivateKey) {
        this.saltSignPrivateKey = saltSignPrivateKey;
    }

    public String getSaltBootPassword() {
        return saltBootPassword;
    }

    public void setSaltBootPassword(String saltBootPassword) {
        this.saltBootPassword = saltBootPassword;
    }

    public String getSaltBootSignPrivateKey() {
        return saltBootSignPrivateKey;
    }

    public void setSaltBootSignPrivateKey(String saltBootSignPrivateKey) {
        this.saltBootSignPrivateKey = saltBootSignPrivateKey;
    }
}
