package com.sequenceiq.freeipa.flow.stack.modify.tags;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class ModifyUserDefinedTagsAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.MODIFY_USER_DEFINED_TAGS;
    }
}
