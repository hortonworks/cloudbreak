package com.sequenceiq.sdx.api.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxRecommendationResponse {

    @Schema(description = ModelDescriptions.TEMPLATE_DETAILS)
    private StackV4Request template;

    @Schema(description = ModelDescriptions.AVAILABLE_VM_TYPES)
    private Map<String, List<VmTypeResponse>> availableVmTypesByInstanceGroup = new HashMap<>();

    public SdxRecommendationResponse() {
    }

    public SdxRecommendationResponse(StackV4Request template, Map<String, List<VmTypeResponse>> availableVmTypesByInstanceGroup) {
        this.template = template;
        this.availableVmTypesByInstanceGroup = availableVmTypesByInstanceGroup;
    }

    public StackV4Request getTemplate() {
        return template;
    }

    public void setTemplate(StackV4Request template) {
        this.template = template;
    }

    public Map<String, List<VmTypeResponse>> getAvailableVmTypesByInstanceGroup() {
        return availableVmTypesByInstanceGroup;
    }

    public void setAvailableVmTypesByInstanceGroup(Map<String, List<VmTypeResponse>> availableVmTypesByInstanceGroup) {
        this.availableVmTypesByInstanceGroup = availableVmTypesByInstanceGroup;
    }

    @Override
    public String toString() {
        return "SdxRecommendationResponse{" + "template=" + template + ", availableVmTypesByInstanceGroup=" + availableVmTypesByInstanceGroup + '}';
    }
}
