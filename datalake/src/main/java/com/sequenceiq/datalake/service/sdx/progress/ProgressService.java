package com.sequenceiq.datalake.service.sdx.progress;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.sdx.api.model.SdxProgressListResponse;
import com.sequenceiq.sdx.api.model.SdxProgressResponse;

@Component
public class ProgressService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressService.class);

    @Inject
    private ProgressV4Endpoint progressV4Endpoint;

    @Inject
    private FlowService flowService;

    public SdxProgressResponse getLastFlowProgressByResourceCrn(String resourceCrn) {
        SdxProgressResponse response = new SdxProgressResponse();
        response.setLastFlowOperation(flowService.getLastFlowProgressByResourceCrn(resourceCrn));
        try {
            response.setLastInternalFlowOperation(progressV4Endpoint.getLastFlowLogProgressByResourceCrn(resourceCrn));
        } catch (NotFoundException notFoundException) {
            LOGGER.debug("Stack for datalake '{}' has not found yet. It is acceptable for progress response.", resourceCrn);
        }
        return response;
    }

    public SdxProgressListResponse getFlowProgressListByResourceCrn(String resourceCrn) {
        SdxProgressListResponse response = new SdxProgressListResponse();
        response.setRecentFlowOperations(flowService.getFlowProgressListByResourceCrn(resourceCrn));
        try {
            response.setRecentInternalFlowOperations(progressV4Endpoint.getFlowLogsProgressByResourceCrn(resourceCrn));
        } catch (NotFoundException notFoundException) {
            LOGGER.debug("Stack for datalake '{}' has not found yet. It is acceptable for progress response.", resourceCrn);
        }
        return response;
    }
}
