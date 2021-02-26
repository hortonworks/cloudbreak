package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.TargetGroupTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.TargetGroupType;

@Entity
public class TargetGroup implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "targetgroup_generator")
    @SequenceGenerator(name = "targetgroup_generator", sequenceName = "targetgroup_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = TargetGroupTypeConverter.class)
    private TargetGroupType type;

    /**
     * @deprecated Use {@link #loadBalancerSet} instead.
     */
    @Deprecated
    @ManyToOne(fetch = FetchType.LAZY)
    private LoadBalancer loadBalancer;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<LoadBalancer> loadBalancerSet = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

    public Long getId() {
        return id;
    }

    public TargetGroupType getType() {
        return type;
    }

    public void setType(TargetGroupType type) {
        this.type = type;
    }

    /**
     * @deprecated Use {@link #getLoadBalancerSet()} instead.
     */
    @Deprecated
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public Set<LoadBalancer> getLoadBalancerSet() {
        if (loadBalancerSet != null) {
            return loadBalancerSet;
        } else {
            return Set.of(loadBalancer);
        }
    }

    /**
     * @deprecated Use {@link #setLoadBalancerSet()} instead.
     */
    @Deprecated
    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public void setLoadBalancerSet(Set<LoadBalancer> loadBalancerSet) {
        this.loadBalancerSet = loadBalancerSet;
        this.loadBalancer = loadBalancerSet.iterator().next();
    }

    public void addLoadBalancer(LoadBalancer loadBalancer) {
        loadBalancerSet.add(loadBalancer);
        this.loadBalancer = loadBalancer;
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TargetGroup that = (TargetGroup) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
