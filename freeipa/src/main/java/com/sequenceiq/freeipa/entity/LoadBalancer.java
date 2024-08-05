package com.sequenceiq.freeipa.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

@Entity
public class LoadBalancer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "loadbalancer_generator")
    @SequenceGenerator(name = "loadbalancer_generator", sequenceName = "loadbalancer_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "stack_id")
    private long stackId;

    @Column(name = "resource_id")
    private String resourceId;

    @OneToMany(mappedBy = "loadBalancer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<TargetGroup> targetGroups = new HashSet<>();

    private String ip;

    private String fqdn;

    @Column(name = "hosted_zone_id")
    private String hostedZoneId;

    private String endpoint;

    private String dns;

    @Override
    public String toString() {
        return new StringJoiner(", ", LoadBalancer.class.getSimpleName() + "[", "]")
                .add("id=" + id)
                .add("stackId=" + stackId)
                .add("resourceId='" + resourceId + "'")
                .add("targetGroups=" + targetGroups)
                .add("ip='" + ip + "'")
                .add("fqdn='" + fqdn + "'")
                .add("hostedZoneId='" + hostedZoneId + "'")
                .add("endpoint='" + endpoint + "'")
                .add("dns='" + dns + "'")
                .toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getStackId() {
        return stackId;
    }

    public void setStackId(long stackId) {
        this.stackId = stackId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Set<TargetGroup> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(Set<TargetGroup> targetGroups) {
        this.targetGroups = targetGroups;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }
}
