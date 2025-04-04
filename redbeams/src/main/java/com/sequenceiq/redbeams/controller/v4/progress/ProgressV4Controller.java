package com.sequenceiq.redbeams.controller.v4.progress;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.service.FlowProgressService;
import com.sequenceiq.redbeams.api.endpoint.v4.progress.ProgressV4Endpoint;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class ProgressV4Controller implements ProgressV4Endpoint {

    @Inject
    private FlowProgressService progressService;

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATABASE_SERVER)
    public FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATABASE_SERVER)
    public List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getFlowProgressListByResourceCrn(resourceCrn);
    }
}
