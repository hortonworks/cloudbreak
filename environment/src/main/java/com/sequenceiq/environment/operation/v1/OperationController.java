package com.sequenceiq.environment.operation.v1;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.environment.api.v1.operation.endpoint.OperationEndpoint;
import com.sequenceiq.environment.operation.service.OperationService;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.service.FlowService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class OperationController implements OperationEndpoint {

    private final OperationService operationService;

    private final FlowService flowService;

    public OperationController(OperationService operationService, FlowService flowService) {
        this.operationService = operationService;
        this.flowService = flowService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public OperationView getOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public OperationStatusResponse getOperationStatus(@ResourceCrn String resourceCrn, String operationId) {
        return flowService.getOperationStatus(resourceCrn, operationId);
    }
}
