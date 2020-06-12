package com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.DiskV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.GatewayRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.InstanceCountV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.ResizeRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.common.model.JsonEntity;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RecommendationV4Response implements JsonEntity {

    private Map<String, VmTypeV4Response> recommendations;

    private Set<VmTypeV4Response> virtualMachines;

    private Set<DiskV4Response> diskResponses;

    private Map<String, InstanceCountV4Response> instanceCounts;

    private GatewayRecommendationV4Response gatewayRecommendation;

    private AutoscaleRecommendationV4Response autoscaleRecommendation;

    private ResizeRecommendationV4Response resizeRecommendation;

    public RecommendationV4Response() {
    }

    public RecommendationV4Response(
            Map<String, VmTypeV4Response> recommendations,
            Set<VmTypeV4Response> virtualMachines,
            Set<DiskV4Response> diskResponses,
            Map<String, InstanceCountV4Response> instanceCounts,
            GatewayRecommendationV4Response gatewayRecommendation,
            AutoscaleRecommendationV4Response autoscaleRecommendation,
            ResizeRecommendationV4Response resizeRecommendation
    ) {
        this.recommendations = recommendations;
        this.virtualMachines = virtualMachines;
        this.diskResponses = diskResponses;
        this.instanceCounts = instanceCounts;
        this.gatewayRecommendation = gatewayRecommendation;
        this.autoscaleRecommendation = autoscaleRecommendation;
        this.resizeRecommendation = resizeRecommendation;
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

    public Map<String, InstanceCountV4Response> getInstanceCounts() {
        return instanceCounts;
    }

    public GatewayRecommendationV4Response getGatewayRecommendation() {
        return gatewayRecommendation;
    }

    public AutoscaleRecommendationV4Response getAutoscaleRecommendation() {
        return autoscaleRecommendation;
    }

    public ResizeRecommendationV4Response getResizeRecommendation() {
        return resizeRecommendation;
    }
}
