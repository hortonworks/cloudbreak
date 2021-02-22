package com.sequenceiq.datalake.controller.progress;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.datalake.service.sdx.progress.ProgressService;
import com.sequenceiq.sdx.api.endpoint.ProgressEndpoint;
import com.sequenceiq.sdx.api.model.SdxProgressListResponse;
import com.sequenceiq.sdx.api.model.SdxProgressResponse;

@Controller
public class ProgressController implements ProgressEndpoint {

    @Inject
    private ProgressService progressService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public SdxProgressResponse getLastFlowLogProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public SdxProgressListResponse getFlowLogsProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
