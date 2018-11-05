package com.sequenceiq.periscope.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
@NamedQueries(@NamedQuery(name = "SecurityConfig.findByClusterId", query = "SELECT s FROM SecurityConfig s WHERE s.cluster.id= :id"))
public class SecurityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    private Long id;

    private String clientKey;

    private String clientCert;

    @Column(columnDefinition = "TEXT")
    private String serverCert;

    @OneToOne
    private Cluster cluster;

    public SecurityConfig() {
    }

    public SecurityConfig(String clientKey, String clientCert, String serverCert) {
        this.clientKey = clientKey;
        this.clientCert = clientCert;
        this.serverCert = serverCert;
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

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public void update(SecurityConfig updatedConfig) {
        clientCert = updatedConfig.clientCert;
        clientKey = updatedConfig.clientKey;
        serverCert = updatedConfig.serverCert;
    }
}
