package com.sequenceiq.environment.environment.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

import com.sequenceiq.cloudbreak.common.database.StringSetToStringConverter;

@Embeddable
public class DefaultComputeCluster implements Serializable {

    @Column(nullable = false, name = "compute_create")
    private boolean create;

    @Column(nullable = false, name = "compute_private_cluster")
    private boolean privateCluster;

    @Column(name = "compute_kube_api_authorized_ip_ranges")
    @Convert(converter = StringSetToStringConverter.class)
    private Set<String> kubeApiAuthorizedIpRanges = new HashSet<>();

    @Column(name = "compute_outbound_type")
    private String outboundType;

    @Column(name = "compute_worker_node_subnets")
    @Convert(converter = StringSetToStringConverter.class)
    private Set<String> workerNodeSubnetIds = new HashSet<>();

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
        return kubeApiAuthorizedIpRanges;
    }

    public void setKubeApiAuthorizedIpRanges(Set<String> kubeApiAuthorizedIpRanges) {
        this.kubeApiAuthorizedIpRanges = kubeApiAuthorizedIpRanges;
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public Set<String> getWorkerNodeSubnetIds() {
        return workerNodeSubnetIds;
    }

    public void setWorkerNodeSubnetIds(Set<String> workerNodeSubnetIds) {
        this.workerNodeSubnetIds = workerNodeSubnetIds;
    }

    @Override
    public String toString() {
        return "DefaultComputeCluster{" +
                "create=" + create +
                ", privateCluster=" + privateCluster +
                ", kubeApiAuthorizedIpRanges='" + kubeApiAuthorizedIpRanges + '\'' +
                ", outboundType='" + outboundType + '\'' +
                ", workerNodeSubnetIds='" + workerNodeSubnetIds + '\'' +
                '}';
    }
}
