package com.sequenceiq.datalake.controller.operation;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.datalake.service.sdx.operation.OperationService;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;

@Controller
public class OperationController implements OperationEndpoint {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public OperationView getOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }
}
