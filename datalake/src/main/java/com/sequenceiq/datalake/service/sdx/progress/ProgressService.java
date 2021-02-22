package com.sequenceiq.datalake.service.sdx.progress;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.sdx.api.model.SdxProgressListResponse;
import com.sequenceiq.sdx.api.model.SdxProgressResponse;

@Component
public class ProgressService {

    @Inject
    private ProgressV4Endpoint progressV4Endpoint;

    @Inject
    private FlowService flowService;

    public SdxProgressResponse getLastFlowProgressByResourceCrn(String resourceCrn) {
        SdxProgressResponse response = new SdxProgressResponse();
        response.setLastFlowOperation(flowService.getLastFlowProgressByResourceCrn(resourceCrn));
        response.setLastInternalFlowOperation(progressV4Endpoint.getLastFlowLogProgressByResourceCrn(resourceCrn));
        return response;
    }

    public SdxProgressListResponse getFlowProgressListByResourceCrn(String resourceCrn) {
        SdxProgressListResponse response = new SdxProgressListResponse();
        response.setRecentFlowOperations(flowService.getFlowProgressListByResourceCrn(resourceCrn));
        response.setRecentInternalFlowOperations(progressV4Endpoint.getFlowLogsProgressByResourceCrn(resourceCrn));
        return response;
    }
}
