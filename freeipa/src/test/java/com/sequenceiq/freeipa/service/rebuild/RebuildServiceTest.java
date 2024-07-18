package com.sequenceiq.freeipa.service.rebuild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Response;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RebuildServiceTest {

    private static final String ACCOUNT_ID = "accId";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private StackService stackService;

    @Mock
    private RebuildRequestValidator rebuildRequestValidator;

    @Mock
    private OperationService operationService;

    @Mock
    private OperationToOperationStatusConverter operationConverter;

    @InjectMocks
    private RebuildService underTest;

    @Test
    void rebuild() {
        RebuildV2Request request = new RebuildV2Request();
        request.setEnvironmentCrn("envCrn");
        request.setInstanceToRestoreFqdn("FQDN");
        request.setDataBackupStorageLocation("dbackup");
        request.setFullBackupStorageLocation("fbackup");
        Stack stack = new Stack();
        stack.setId(56L);
        stack.setCloudPlatform(CloudPlatform.MOCK.name());
        when(stackService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        when(entitlementService.isFreeIpaRebuildEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);
        Operation operation = new Operation();
        operation.setOperationId(UUID.randomUUID().toString());
        when(operationService.startOperation(ACCOUNT_ID, OperationType.REBUILD, Set.of(request.getEnvironmentCrn()), Set.of()))
                .thenReturn(operation);

        RebuildV2Response result = underTest.rebuild(ACCOUNT_ID, request);

        assertEquals(request.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(request.getInstanceToRestoreFqdn(), result.getInstanceToRestoreFqdn());
        assertEquals(request.getDataBackupStorageLocation(), result.getDataBackupStorageLocation());
        assertEquals(request.getFullBackupStorageLocation(), result.getFullBackupStorageLocation());
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq(EventSelectorUtil.selector(RebuildEvent.class)), captor.capture());
        RebuildEvent rebuildEvent = (RebuildEvent) captor.getValue();
        assertEquals(stack.getId(), rebuildEvent.getResourceId());
        assertEquals(request.getInstanceToRestoreFqdn(), rebuildEvent.getInstanceToRestoreFqdn());
        assertEquals(request.getDataBackupStorageLocation(), rebuildEvent.getDataBackupStorageLocation());
        assertEquals(request.getFullBackupStorageLocation(), rebuildEvent.getFullBackupStorageLocation());
        assertEquals(operation.getOperationId(), rebuildEvent.getOperationId());
    }

    @Test
    void rebuildFailsOnEntitlement() {
        when(entitlementService.isFreeIpaRebuildEnabled(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        RebuildV2Request request = new RebuildV2Request();

        assertThrows(BadRequestException.class, () -> underTest.rebuild(ACCOUNT_ID, request));
    }
}