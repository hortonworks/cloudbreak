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
import javax.persistence.Table;


@Entity
@Table(name = "SecurityConfig")
@NamedQueries({
        @NamedQuery(name = "SecurityConfig.findByClusterId", query = "SELECT s FROM SecurityConfig s WHERE s.cluster.id= :id"),
})
public class SecurityConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_table")
    private Long id;
    @Column
    private byte[] clientKey;
    @Column
    private byte[] clientCert;
    @Column
    private byte[] serverCert;

    @OneToOne
    private Cluster cluster;

    public SecurityConfig() {
    }

    public SecurityConfig(byte[] clientKey, byte[] clientCert, byte[] serverCert) {
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

    public byte[] getClientKey() {
        return clientKey;
    }

    public void setClientKey(byte[] clientKey) {
        this.clientKey = clientKey;
    }

    public byte[] getClientCert() {
        return clientCert;
    }

    public void setClientCert(byte[] clientCert) {
        this.clientCert = clientCert;
    }

    public byte[] getServerCert() {
        return serverCert;
    }

    public void setServerCert(byte[] serverCert) {
        this.serverCert = serverCert;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public void update(SecurityConfig updatedConfig){
        this.setClientCert(updatedConfig.getClientCert());
        this.setClientKey(updatedConfig.getClientKey());
        this.setServerCert(updatedConfig.getServerCert());
    }
}
