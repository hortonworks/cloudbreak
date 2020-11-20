package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Entity
public class TargetGroup implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "targetgroup_generator")
    @SequenceGenerator(name = "targetgroup_generator", sequenceName = "targetgroup_id_seq", allocationSize = 1)
    private Long id;

    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    private LoadBalancer loadBalancer;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<InstanceGroup> instanceGroups;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }
}
