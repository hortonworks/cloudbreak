package com.sequenceiq.cloudbreak.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationResponse implements JsonEntity {

    private Map<String, VmTypeJson> recommendations = new HashMap<>();

    private Set<VmTypeJson> virtualMachines = new HashSet<>();

    private Set<DiskResponse> diskResponses = new HashSet<>();

    public RecommendationResponse(Map<String, VmTypeJson> recommendations, Set<VmTypeJson> virtualMachines, Set<DiskResponse> diskResponses) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
        this.diskResponses = diskResponses;
    }

    public Map<String, VmTypeJson> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Map<String, VmTypeJson> recommendations) {
        this.recommendations = recommendations;
    }

    public Set<VmTypeJson> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmTypeJson> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public Set<DiskResponse> getDiskResponses() {
        return diskResponses;
    }

    public void setDiskResponses(Set<DiskResponse> diskResponses) {
        this.diskResponses = diskResponses;
    }
}
