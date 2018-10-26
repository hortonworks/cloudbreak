package com.sequenceiq.periscope.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.apache.commons.codec.binary.Base64;

import com.sequenceiq.cloudbreak.domain.converter.EncryptionConverter;

@Entity
@NamedQueries(@NamedQuery(name = "SecurityConfig.findByClusterId", query = "SELECT s FROM SecurityConfig s WHERE s.cluster.id= :id"))
public class SecurityConfig implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityconfig_generator")
    @SequenceGenerator(name = "securityconfig_generator", sequenceName = "securityconfig_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = EncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String clientKey;

    @Convert(converter = EncryptionConverter.class)
    @Column(columnDefinition = "TEXT")
    private String clientCert;

    @Convert(converter = EncryptionConverter.class)
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

    public String getClientKeyDecoded() {
        return clientKey == null ? null : new String(Base64.decodeBase64(clientKey));
    }

    public String getClientCertDecoded() {
        return clientCert == null ? null : new String(Base64.decodeBase64(clientCert));
    }

    public String getServerCertDecoded() {
        return serverCert == null ? null : new String(Base64.decodeBase64(serverCert));
    }

    @Override
    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getClientCert() {
        return clientCert;
    }

    public String getServerCert() {
        return serverCert;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public void setClientCert(String clientCert) {
        this.clientCert = clientCert;
    }

    public void setServerCert(String serverCert) {
        this.serverCert = serverCert;
    }

    public void update(SecurityConfig updatedConfig) {
        clientCert = updatedConfig.clientCert;
        clientKey = updatedConfig.clientKey;
        serverCert = updatedConfig.serverCert;
    }
}
