package com.sequenceiq.datalake.controller.operation;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.datalake.service.sdx.operation.OperationService;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;

@Controller
public class OperationController implements OperationEndpoint {

    private final OperationService operationService;

    private final FlowService flowService;

    public OperationController(OperationService operationService, FlowService flowService) {
        this.operationService = operationService;
        this.flowService = flowService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public OperationView getOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public OperationStatusResponse getOperationStatus(@ResourceCrn String resourceCrn, String operationId) {
        return flowService.getOperationStatus(resourceCrn, operationId);
    }
}
