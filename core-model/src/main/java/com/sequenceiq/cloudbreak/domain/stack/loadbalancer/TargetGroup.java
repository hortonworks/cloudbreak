package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
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

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<LoadBalancer> loadBalancerSet = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<InstanceGroup> instanceGroups = new HashSet<>();

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json providerConfig;

    public Long getId() {
        return id;
    }

    public TargetGroupType getType() {
        return type;
    }

    public void setType(TargetGroupType type) {
        this.type = type;
    }

    public Set<LoadBalancer> getLoadBalancerSet() {
        return loadBalancerSet;
    }

    public void setLoadBalancerSet(Set<LoadBalancer> loadBalancerSet) {
        this.loadBalancerSet = loadBalancerSet;
    }

    public void addLoadBalancer(LoadBalancer loadBalancer) {
        loadBalancerSet.add(loadBalancer);
    }

    public Set<InstanceGroup> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroup> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public TargetGroupConfigDbWrapper getProviderConfig() {
        if (providerConfig != null && providerConfig.getValue() != null) {
            return JsonUtil.readValueOpt(providerConfig.getValue(), TargetGroupConfigDbWrapper.class).orElse(null);
        }
        return null;
    }

    public void setProviderConfig(TargetGroupConfigDbWrapper cloudConfig) {
        if (cloudConfig != null) {
            this.providerConfig = new Json(cloudConfig);
        }
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

    @Override
    public String toString() {
        return "TargetGroup{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
