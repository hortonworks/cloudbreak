package com.sequenceiq.freeipa.service.migration;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.service.operation.OperationAcceptor;

@Component
public class MultiAzMigrationOperationAcceptor extends OperationAcceptor {

    @Override
    protected OperationType selector() {
        return OperationType.MIGRATE_TO_MULTI_AZ;
    }
}
