package com.sequenceiq.redbeams.service.progress;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.service.FlowService;

@Component
public class ProgressService {

    private final FlowService flowService;

    public ProgressService(FlowService flowService) {
        this.flowService = flowService;
    }

    public FlowProgressResponse getLastFlowProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    public List<FlowProgressResponse> getFlowProgressListByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
