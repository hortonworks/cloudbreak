package com.sequenceiq.redbeams.controller.v4.progress;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.redbeams.service.progress.ProgressService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class ProgressV4Controller implements ProgressV4Endpoint {

    @Inject
    private ProgressService progressService;

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
