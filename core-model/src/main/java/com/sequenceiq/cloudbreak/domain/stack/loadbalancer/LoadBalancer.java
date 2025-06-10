package com.sequenceiq.cloudbreak.domain.stack.loadbalancer;

import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.converter.LoadBalancerSkuConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.LoadBalancerTypeConverter;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Entity
public class LoadBalancer implements ProvisionEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "loadbalancer_generator")
    @SequenceGenerator(name = "loadbalancer_generator", sequenceName = "loadbalancer_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "stack_id")
    private Long stackId;

    private String dns;

    private String hostedZoneId;

    private String ip;

    @Convert(converter = LoadBalancerTypeConverter.class)
    private LoadBalancerType type;

    private String endpoint;

    @ManyToMany(mappedBy = "loadBalancerSet", fetch = FetchType.LAZY)
    private Set<TargetGroup> targetGroupSet = new HashSet<>();

    private String fqdn;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json providerConfig;

    @Convert(converter = LoadBalancerSkuConverter.class)
    private LoadBalancerSku sku = LoadBalancerSku.getDefault();

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

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
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

    public LoadBalancerType getType() {
        return type;
    }

    public void setType(LoadBalancerType type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Set<TargetGroup> getTargetGroupSet() {
        return targetGroupSet;
    }

    public void setTargetGroupSet(Set<TargetGroup> targetGroups) {
        this.targetGroupSet = targetGroups;
    }

    public void addTargetGroup(TargetGroup targetGroup) {
        targetGroupSet.add(targetGroup);
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public LoadBalancerConfigDbWrapper getProviderConfig() {
        if (providerConfig != null && providerConfig.getValue() != null) {
            return JsonUtil.readValueOpt(providerConfig.getValue(), LoadBalancerConfigDbWrapper.class).orElse(null);
        }
        return null;
    }

    public void setProviderConfig(LoadBalancerConfigDbWrapper cloudConfig) {
        if (cloudConfig != null) {
            this.providerConfig = new Json(cloudConfig);
        }
    }

    public LoadBalancerSku getSku() {
        return sku;
    }

    public void setSku(LoadBalancerSku sku) {
        this.sku = sku;
    }

    @Override
    public String toString() {
        return "LoadBalancer{" +
                "id=" + id +
                ", stackId=" + stackId +
                ", dns='" + dns + '\'' +
                ", hostedZoneId='" + hostedZoneId + '\'' +
                ", ip='" + ip + '\'' +
                ", type=" + type +
                ", endpoint='" + endpoint + '\'' +
                ", targetGroupSet=" + targetGroupSet +
                ", fqdn='" + fqdn + '\'' +
                ", providerConfig=" + providerConfig +
                ", sku=" + sku +
                '}';
    }
}
