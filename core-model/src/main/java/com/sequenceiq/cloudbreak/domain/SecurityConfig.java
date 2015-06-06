package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "SecurityConfig")
public class SecurityConfig implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_table")
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String clientKey;
    @Column(columnDefinition = "TEXT")
    private String clientCert;
    @Column(columnDefinition = "TEXT")
    private String serverCert;
    @Column(columnDefinition = "TEXT")
    private String temporarySshPublicKey;
    @Column(columnDefinition = "TEXT")
    private String temporarySshPrivateKey;
    @OneToOne
    private Stack stack;

    public SecurityConfig() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getClientCert() {
        return clientCert;
    }

    public void setClientCert(String clientCert) {
        this.clientCert = clientCert;
    }

    public String getServerCert() {
        return serverCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public String getTemporarySshPublicKey() {
        return temporarySshPublicKey;
    }

    public void setTemporarySshPublicKey(String temporarySshPublicKey) {
        this.temporarySshPublicKey = temporarySshPublicKey;
    }

    public String getTemporarySshPrivateKey() {
        return temporarySshPrivateKey;
    }

    public void setTemporarySshPrivateKey(String temporarySshPrivateKey) {
        this.temporarySshPrivateKey = temporarySshPrivateKey;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }
}
