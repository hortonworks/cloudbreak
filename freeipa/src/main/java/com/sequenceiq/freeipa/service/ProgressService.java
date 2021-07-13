package com.sequenceiq.freeipa.service;

import java.util.List;

import org.springframework.stereotype.Component;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.service.FlowService;

@Component
public class ProgressService {

    private final FlowService flowService;

    public ProgressService(FlowService flowService) {
        this.flowService = flowService;
    }

    public FlowProgressResponse getLastFlowProgressByResourceCrn(String resourceCrn) {
        return flowService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    public List<FlowProgressResponse> getFlowProgressListByResourceCrn(String resourceCrn) {
        return flowService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
