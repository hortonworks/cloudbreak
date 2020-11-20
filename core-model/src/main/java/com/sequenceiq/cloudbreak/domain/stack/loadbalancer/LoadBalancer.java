package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Entity
public class LoadBalancer implements ProvisionEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "loadbalancer_generator")
    @SequenceGenerator(name = "loadbalancer_generator", sequenceName = "loadbalancer_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Stack stack;

    private String dns;

    private String hostedZoneId;

    private String ip;

    private String type;

    private String endpoint;

    @OneToMany(mappedBy = "loadBalancer", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<TargetGroup> targetGroups = new HashSet<>();

    public Long getId() {
        return id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getDns() {
        return dns;
    }

    public void setDns(String dns) {
        this.dns = dns;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Set<TargetGroup> getTargetGroups() {
        return targetGroups;
    }

    public void setTargetGroups(Set<TargetGroup> targetGroups) {
        this.targetGroups = targetGroups;
    }

    @Override
    public String toString() {
        return "LoadBalancer{" +
            "id=" + id +
            ", dns='" + dns + '\'' +
            ", hostedZoneId='" + hostedZoneId + '\'' +
            ", type='" + type + '\'' +
            ", endpoint='" + endpoint + '\'' +
            '}';
    }
}
