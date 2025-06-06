package com.sequenceiq.redbeams.controller.v4.operation;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.flow.api.model.operation.OperationStatusResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.redbeams.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.redbeams.service.operation.OperationService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class OperationV4Controller implements OperationV4Endpoint {

    private final OperationService operationService;

    private final FlowService flowService;

    public OperationV4Controller(OperationService operationService, FlowService flowService) {
        this.operationService = operationService;
        this.flowService = flowService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATABASE_SERVER)
    public OperationView getRedbeamsOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATABASE_SERVER)
    public OperationStatusResponse getOperationStatus(@ResourceCrn String resourceCrn, String operationId) {
        return flowService.getOperationStatus(resourceCrn, operationId);
    }
}
