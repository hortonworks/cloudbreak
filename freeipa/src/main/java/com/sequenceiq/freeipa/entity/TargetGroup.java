package com.sequenceiq.freeipa.entity;

import java.util.StringJoiner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity
public class TargetGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "targetgroup_generator")
    @SequenceGenerator(name = "targetgroup_generator", sequenceName = "targetgroup_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private LoadBalancer loadBalancer;

    @Column(name = "traffic_port")
    private Integer trafficPort;

    private String protocol;

    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public Integer getTrafficPort() {
        return trafficPort;
    }

    public void setTrafficPort(Integer trafficPort) {
        this.trafficPort = trafficPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TargetGroup.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("trafficPort=" + trafficPort)
                .add("protocol='" + protocol + "'")
                .toString();
    }
}
