package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.TargetGroupTypeConverter;
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

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json providerConfig;

    @Column(nullable = false, name = "use_sticky_session")
    private Boolean useStickySession = Boolean.FALSE;

    public Long getId() {
        return id;
    }

    /**
     * Need this for Jackson deserialization
     * @param id entity id
     */
    private void setId(Long id) {
        this.id = id;
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

    public TargetGroupConfigDbWrapper getProviderConfig() {
        if (providerConfig != null && providerConfig.getValue() != null) {
            return JsonUtil.readValueOpt(providerConfig.getValue(), TargetGroupConfigDbWrapper.class).orElse(null);
        }
        return null;
    }

    public Boolean isUseStickySession() {
        return Boolean.TRUE.equals(useStickySession);
    }

    public void setUseStickySession(Boolean useStickySession) {
        this.useStickySession = useStickySession;
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
}
