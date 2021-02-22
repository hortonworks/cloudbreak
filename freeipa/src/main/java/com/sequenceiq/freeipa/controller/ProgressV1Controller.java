package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.freeipa.api.v1.progress.ProgressV1Endpoint;

@Controller
public class ProgressV1Controller implements ProgressV1Endpoint {

    @Inject
    private FlowService flowService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_ENVIRONMENT)
    public List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return flowService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
