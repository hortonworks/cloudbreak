package com.sequenceiq.periscope.model.yarn;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class YarnScalingServiceV1Response {

    private static final String YARN_RESPONSE_NM_CANDIDATES_KEY = "newNMCandidates";

    private static final String YARN_RESPONSE_DECOMMISSION_CANDIDATES_KEY = "candidates";

    private String apiVersion;

    private String consideredResourceTypes;

    @JsonIgnore
    private Map<String, List<HostGroupInstanceType>> nodeInstanceTypeList;

    private Map<String, NewNodeManagerCandidates> newNMCandidates;

    private Map<String, List<DecommissionCandidate>> decommissionCandidates;

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getConsideredResourceTypes() {
        return consideredResourceTypes;
    }

    public void setConsideredResourceTypes(String consideredResourceTypes) {
        this.consideredResourceTypes = consideredResourceTypes;
    }

    public Map<String, NewNodeManagerCandidates> getNewNMCandidates() {
        return newNMCandidates == null ? Map.of() : newNMCandidates;
    }

    public void setNewNMCandidates(Map<String, NewNodeManagerCandidates> newNMCandidates) {
        this.newNMCandidates = newNMCandidates;
    }

    public Map<String, List<DecommissionCandidate>> getDecommissionCandidates() {
        return decommissionCandidates == null ? Map.of() : decommissionCandidates;
    }

    public void setDecommissionCandidates(Map<String, List<DecommissionCandidate>> decommissionCandidates) {
        this.decommissionCandidates = decommissionCandidates;
    }

    public Map<String, List<HostGroupInstanceType>> getNodeInstanceTypeList() {
        return nodeInstanceTypeList == null ? Map.of() : nodeInstanceTypeList;
    }

    public void setNodeInstanceTypeList(Map<String, List<HostGroupInstanceType>> nodeInstanceTypeList) {
        this.nodeInstanceTypeList = nodeInstanceTypeList;
    }

    public Optional<NewNodeManagerCandidates> getScaleUpCandidates() {
        return Optional.ofNullable(getNewNMCandidates().get(YARN_RESPONSE_NM_CANDIDATES_KEY));
    }

    public Optional<List<DecommissionCandidate>> getScaleDownCandidates() {
        return Optional.ofNullable(getDecommissionCandidates().get(YARN_RESPONSE_DECOMMISSION_CANDIDATES_KEY));
    }

    @Override
    public String toString() {
        return "YarnScalingServiceV1Response{" +
                "apiVersion='" + apiVersion + '\'' +
                ", newNMCandidates=" + newNMCandidates +
                ", decommissionCandidates=" + decommissionCandidates +
                '}';
    }
}

