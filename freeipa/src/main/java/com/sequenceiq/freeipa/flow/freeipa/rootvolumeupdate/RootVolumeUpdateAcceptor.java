package com.sequenceiq.freeipa.flow.freeipa.rootvolumeupdate;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class RootVolumeUpdateAcceptor  extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.MODIFY_ROOT_VOLUME;
    }
}
