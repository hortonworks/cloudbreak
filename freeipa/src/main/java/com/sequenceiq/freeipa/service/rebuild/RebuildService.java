package com.sequenceiq.freeipa.service.rebuild;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Response;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.service.freeipa.backup.cloud.AdlsGen2BackupConfigGenerator;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class RebuildService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebuildService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    @Inject
    private RebuildRequestValidator requestValidator;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationConverter;

    @Inject
    private AdlsGen2BackupConfigGenerator adlsGen2BackupConfigGenerator;

    public RebuildV2Response rebuild(String accountId, RebuildV2Request request) {
        checkEntitlement(accountId);
        Stack stack = stackService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
        requestValidator.validate(request, stack);
        Operation operation = operationService.startOperation(accountId, OperationType.REBUILD, Set.of(request.getEnvironmentCrn()), Set.of());
        RebuildEvent rebuildEvent = new RebuildEvent(stack.getId(), request.getInstanceToRestoreFqdn(),
                getBackupStorageLocation(stack.getCloudPlatform(), request.getFullBackupStorageLocation()),
                getBackupStorageLocation(stack.getCloudPlatform(), request.getDataBackupStorageLocation()), operation.getOperationId());
        FlowIdentifier flowIdentifier = flowManager.notify(rebuildEvent.selector(), rebuildEvent);
        RebuildV2Response response = new RebuildV2Response();
        response.setFlowIdentifier(flowIdentifier);
        response.setOperationStatus(operationConverter.convert(operation));
        BeanUtils.copyProperties(request, response);
        return response;
    }

    private String getBackupStorageLocation(String cloudPlatform, String storageLocation) {
        if (CloudPlatform.AZURE == CloudPlatform.valueOf(cloudPlatform)) {
            return adlsGen2BackupConfigGenerator.convertToRestoreLocation(storageLocation);
        } else {
            return storageLocation;
        }
    }

    private void checkEntitlement(String accountId) {
        if (!entitlementService.isFreeIpaRebuildEnabled(accountId)) {
            throw new BadRequestException("The FreeIPA rebuild capability is disabled.");
        }
    }
}
