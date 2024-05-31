package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.util.CidrUtil;

@Embeddable
public class DefaultComputeCluster implements Serializable {

    @Column(nullable = false, name = "compute_create")
    private boolean create;

    @Column(nullable = false, name = "compute_private_cluster")
    private boolean privateCluster;

    @Column(name = "compute_kube_api_authorized_ip_ranges")
    private String kubeApiAuthorizedIpRanges;

    @Column(name = "compute_outbound_type")
    private String outboundType;

    @Column(name = "compute_load_balancer_authorized_ip_ranges")
    private String loadBalancerAuthorizedIpRanges;

    public DefaultComputeCluster() {
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isPrivateCluster() {
        return privateCluster;
    }

    public void setPrivateCluster(boolean privateCluster) {
        this.privateCluster = privateCluster;
    }

    public Set<String> getKubeApiAuthorizedIpRanges() {
        if (StringUtils.isEmpty(kubeApiAuthorizedIpRanges)) {
            return Set.of();
        } else {
            return CidrUtil.cidrSet(kubeApiAuthorizedIpRanges);
        }
    }

    public void setKubeApiAuthorizedIpRanges(Set<String> kubeApiAuthorizedIpRanges) {
        if (CollectionUtils.isEmpty(kubeApiAuthorizedIpRanges)) {
            this.kubeApiAuthorizedIpRanges = null;
        } else {
            this.kubeApiAuthorizedIpRanges = StringUtils.join(kubeApiAuthorizedIpRanges, ",");
        }
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public Set<String> getLoadBalancerAuthorizedIpRanges() {
        if (StringUtils.isEmpty(loadBalancerAuthorizedIpRanges)) {
            return Set.of();
        } else {
            return CidrUtil.cidrSet(loadBalancerAuthorizedIpRanges);
        }
    }

    public void setLoadBalancerAuthorizedIpRanges(Set<String> loadBalancerAuthorizedIpRanges) {
        if (CollectionUtils.isEmpty(loadBalancerAuthorizedIpRanges)) {
            this.loadBalancerAuthorizedIpRanges = null;
        } else {
            this.loadBalancerAuthorizedIpRanges = StringUtils.join(loadBalancerAuthorizedIpRanges, ",");
        }
    }

    @Override
    public String toString() {
        return "DefaultComputeCluster{" +
                "create=" + create +
                ", privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", loadBalancerAuthorizedIpRanges='" + loadBalancerAuthorizedIpRanges + '\'' +
                '}';
    }
}
