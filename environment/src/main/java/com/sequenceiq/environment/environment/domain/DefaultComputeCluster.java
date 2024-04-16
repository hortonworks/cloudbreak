package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

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

    public String getKubeApiAuthorizedIpRanges() {
        return kubeApiAuthorizedIpRanges;
    }

    public void setKubeApiAuthorizedIpRanges(String kubeApiAuthorizedIpRanges) {
        this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public String getLoadBalancerAuthorizedIpRanges() {
        return loadBalancerAuthorizedIpRanges;
    }

    public void setLoadBalancerAuthorizedIpRanges(String loadBalancedAuthorizedIpRanges) {
        this.loadBalancerAuthorizedIpRanges = loadBalancedAuthorizedIpRanges;
    }

    @Override
    public String toString() {
        return "DefaultComputeCluster{" +
                "create=" + create +
                ", privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", loadBalancedAuthorizedIpRanges='" + loadBalancerAuthorizedIpRanges + '\'' +
                '}';
    }
}
