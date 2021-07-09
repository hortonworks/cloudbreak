package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.cloudbreak.service.progress.ProgressService;
import com.sequenceiq.flow.api.model.FlowProgressResponse;

@Controller
public class ProgressV4Controller implements ProgressV4Endpoint {

    @Inject
    private ProgressService progressService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
