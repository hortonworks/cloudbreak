package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.service.operation.OperationService;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.service.FlowService;

@Controller
public class OperationV4Controller implements OperationV4Endpoint {

    private final OperationService operationService;

    private final FlowService flowService;

    public OperationV4Controller(OperationService operationService, FlowService flowService) {
        this.operationService = operationService;
        this.flowService = flowService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public OperationView getOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public OperationStatusResponse getOperationStatus(@ResourceCrn String resourceCrn, String operationId) {
        return flowService.getOperationStatus(resourceCrn, operationId);
    }

}
