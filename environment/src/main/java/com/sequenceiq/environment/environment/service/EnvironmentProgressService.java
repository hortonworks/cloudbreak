package com.sequenceiq.environment.environment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.sequenceiq.flow.converter.OperationDetailsPopulator;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.service.FlowService;

@Service
public class EnvironmentProgressService {

    private final EnvironmentService environmentService;

    private final FlowService flowService;

    private final FreeIpaService freeIpaService;

    private final OperationDetailsPopulator operationDetailsPopulator;

    public EnvironmentProgressService(EnvironmentService environmentService, FlowService flowService, FreeIpaService freeIpaService,
            OperationDetailsPopulator operationDetailsPopulator) {
        this.environmentService = environmentService;
        this.flowService = flowService;
        this.freeIpaService = freeIpaService;
        this.operationDetailsPopulator = operationDetailsPopulator;
    }

    public FlowProgressResponse getLastFlowProgressByResourceCrn(String resourceCrn) {
        return flowService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    public List<FlowProgressResponse> getFlowProgressListByResourceCrn(String resourceCrn) {
        return flowService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
