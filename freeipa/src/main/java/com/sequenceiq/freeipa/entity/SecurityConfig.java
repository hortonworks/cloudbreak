package com.sequenceiq.freeipa.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
public class SecurityConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String clientKey;

    @Column(columnDefinition = "TEXT")
    private String clientCert;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private SaltSecurityConfig saltSecurityConfig;

    @Column(nullable = false)
    private boolean usePrivateIpToTls;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getClientCert() {
        return clientCert;
    }

    public void setClientCert(String clientCert) {
        this.clientCert = clientCert;
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
}
