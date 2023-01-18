package com.sequenceiq.cloudbreak.api.endpoint.v4.cost.requests;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.api.client.util.Lists;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ClusterCostV4Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterCostV4Request implements Serializable {

    private List<String> environmentCrns = Lists.newArrayList();

    private List<String> clusterCrns = Lists.newArrayList();

    public List<String> getEnvironmentCrns() {
        return environmentCrns;
    }

    public void setEnvironmentCrns(List<String> environmentCrns) {
        this.environmentCrns = environmentCrns;
    }

    public List<String> getClusterCrns() {
        return clusterCrns;
    }

    public void setClusterCrns(List<String> clusterCrns) {
        this.clusterCrns = clusterCrns;
    }

    @Override
    public String toString() {
        return "ClusterCostV4Request{" +
                "environmentCrns=" + environmentCrns +
                ", clusterCrns=" + clusterCrns +
                '}';
    }
}
