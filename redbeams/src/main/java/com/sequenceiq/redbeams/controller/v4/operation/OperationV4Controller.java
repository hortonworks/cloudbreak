package com.sequenceiq.redbeams.controller.v4.operation;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATABASE_SERVER;

import javax.transaction.Transactional;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.redbeams.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.redbeams.service.operation.OperationService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class OperationV4Controller implements OperationV4Endpoint {

    private final OperationService operationService;

    public OperationV4Controller(OperationService operationService) {
        this.operationService = operationService;
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATABASE_SERVER)
    public OperationView getRedbeamsOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }
}
