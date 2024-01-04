package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "gateway_topology")
public class GatewayTopology implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "gateway_topology_generator")
    @SequenceGenerator(name = "gateway_topology_generator", sequenceName = "gateway_topology_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Gateway gateway;

    @Column(nullable = false)
    private String topologyName;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json exposedServices;

    public GatewayTopology copy() {
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.id = id;
        gatewayTopology.topologyName = topologyName;
        gatewayTopology.exposedServices = exposedServices;
        gatewayTopology.gateway = gateway;
        return gatewayTopology;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public void setGateway(Gateway gateway) {
        this.gateway = gateway;
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
}
