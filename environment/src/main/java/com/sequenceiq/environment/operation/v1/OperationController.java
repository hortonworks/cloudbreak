package com.sequenceiq.environment.operation.v1;

import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.environment.api.v1.operation.endpoint.OperationEndpoint;
import com.sequenceiq.environment.operation.service.OperationService;
import com.sequenceiq.flow.api.model.operation.OperationView;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class OperationController implements OperationEndpoint {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public OperationView getOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }
}
