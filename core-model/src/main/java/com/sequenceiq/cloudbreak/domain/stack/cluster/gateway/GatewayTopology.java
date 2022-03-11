package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;

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

    @Override
    public String toString() {
        return "GatewayTopology{" +
                "id=" + id +
                ", topologyName='" + topologyName + '\'' +
                '}';
    }
}
