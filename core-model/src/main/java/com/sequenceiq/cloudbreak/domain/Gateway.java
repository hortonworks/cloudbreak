package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gateway_generator")
    @SequenceGenerator(name = "gateway_generator", sequenceName = "gateway_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Cluster cluster;

    @Column(nullable = false)
    private Boolean enableGateway;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String topologyName;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json exposedServices;

    private String ssoProvider;

    private String signKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public Boolean getEnableGateway() {
        return enableGateway;
    }

    public void setEnableGateway(Boolean enableGateway) {
        this.enableGateway = enableGateway;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTopologyName() {
        return topologyName;
    }

    public void setTopologyName(String topologyName) {
        this.topologyName = topologyName;
    }

    public Json getExposedServices() {
        return exposedServices;
    }

    public void setExposedServices(Json exposedServices) {
        this.exposedServices = exposedServices;
    }

    public String getSsoProvider() {
        return ssoProvider;
    }

    public void setSsoProvider(String ssoProvider) {
        this.ssoProvider = ssoProvider;
    }

    public String getSignKey() {
        return signKey;
    }

    public void setSignKey(String signKey) {
        this.signKey = signKey;
    }
}
