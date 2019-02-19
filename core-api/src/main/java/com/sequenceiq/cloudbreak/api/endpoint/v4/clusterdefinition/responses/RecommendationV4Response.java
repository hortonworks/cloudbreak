package com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.DiskV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RecommendationV4Response implements JsonEntity {

    private Map<String, VmTypeV4Response> recommendations;

    private Set<VmTypeV4Response> virtualMachines;

    private Set<DiskV4Response> diskResponses;

    public RecommendationV4Response() {
    }

    public RecommendationV4Response(Map<String, VmTypeV4Response> recommendations, Set<VmTypeV4Response> virtualMachines, Set<DiskV4Response> diskResponses) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
        this.diskResponses = diskResponses;
    }

    public Map<String, VmTypeV4Response> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(Map<String, VmTypeV4Response> recommendations) {
        this.recommendations = recommendations;
    }

    public Set<VmTypeV4Response> getVirtualMachines() {
        return virtualMachines;
    }

    public void setVirtualMachines(Set<VmTypeV4Response> virtualMachines) {
        this.virtualMachines = virtualMachines;
    }

    public Set<DiskV4Response> getDiskResponses() {
        return diskResponses;
    }

    public void setDiskResponses(Set<DiskV4Response> diskResponses) {
        this.diskResponses = diskResponses;
    }
}
