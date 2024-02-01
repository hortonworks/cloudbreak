package com.sequenceiq.freeipa.service.rebuild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
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
        when(stackService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), ACCOUNT_ID)).thenReturn(stack);
        when(entitlementService.isFreeIpaRebuildEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);

        DescribeFreeIpaResponse result = underTest.rebuild(ACCOUNT_ID, request);

        assertNull(result);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq(EventSelectorUtil.selector(RebuildEvent.class)), captor.capture());
        RebuildEvent rebuildEvent = (RebuildEvent) captor.getValue();
        assertEquals(stack.getId(), rebuildEvent.getResourceId());
        assertEquals(request.getInstanceToRestoreFqdn(), rebuildEvent.getInstanceToRestoreFqdn());
        assertEquals(request.getDataBackupStorageLocation(), rebuildEvent.getDataBackupStorageLocation());
        assertEquals(request.getFullBackupStorageLocation(), rebuildEvent.getFullBackupStorageLocation());
    }

    @Test
    void rebuildFailsOnEntitlement() {
        when(entitlementService.isFreeIpaRebuildEnabled(ACCOUNT_ID)).thenReturn(Boolean.FALSE);
        RebuildV2Request request = new RebuildV2Request();

        assertThrows(BadRequestException.class, () -> underTest.rebuild(ACCOUNT_ID, request));
    }
}