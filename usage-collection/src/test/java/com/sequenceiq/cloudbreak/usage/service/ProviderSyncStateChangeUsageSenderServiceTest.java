package com.sequenceiq.cloudbreak.usage.service;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPProviderSyncStateOperation.Value.ADDED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPProviderSyncStateOperation.Value.REMOVED;
import static com.sequenceiq.common.model.ProviderSyncState.OUTBOUND_UPGRADE_NEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class ProviderSyncStateChangeUsageSenderServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:iam:us-west-1:1234:user:abcd";

    private static final String ACCOUNT_ID = "1234";

    @InjectMocks
    private ProviderSyncStateChangeUsageSenderService underTest;

    @Mock
    private UsageReporter usageReporter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddProviderSyncStateShouldSendAddedUsageReport() {
        underTest.addProviderSyncState(OUTBOUND_UPGRADE_NEEDED, RESOURCE_CRN);

        ArgumentCaptor<UsageProto.CDPProviderSyncStateChange> captor = ArgumentCaptor.forClass(UsageProto.CDPProviderSyncStateChange.class);
        verify(usageReporter, times(1)).cdpProviderSyncStateChange(captor.capture());

        UsageProto.CDPProviderSyncStateChange report = captor.getValue();
        assertEquals(report.getAccountId(), ACCOUNT_ID);
        assertEquals(report.getSyncState(), "OUTBOUND_UPGRADE_NEEDED");
        assertEquals(report.getResourceCrn(), RESOURCE_CRN);
        assertTrue(report.getReason().isEmpty());
        assertEquals(report.getOperation(), ADDED);
    }

    @Test
    void testRemoveProviderSyncStateShouldSendRemovedUsageReport() {
        underTest.removeProviderSyncState(OUTBOUND_UPGRADE_NEEDED, RESOURCE_CRN);

        ArgumentCaptor<UsageProto.CDPProviderSyncStateChange> captor = ArgumentCaptor.forClass(UsageProto.CDPProviderSyncStateChange.class);
        verify(usageReporter, times(1)).cdpProviderSyncStateChange(captor.capture());

        UsageProto.CDPProviderSyncStateChange report = captor.getValue();
        assertEquals(report.getAccountId(), ACCOUNT_ID);
        assertEquals(report.getSyncState(), "OUTBOUND_UPGRADE_NEEDED");
        assertEquals(report.getResourceCrn(), RESOURCE_CRN);
        assertTrue(report.getReason().isEmpty());
        assertEquals(report.getOperation(), REMOVED);
    }

    @Test
    void testSendUsageReportShouldHandleExceptionGracefully() {
        doThrow(new RuntimeException("Boom")).when(usageReporter).cdpProviderSyncStateChange(any());

        underTest.addProviderSyncState(OUTBOUND_UPGRADE_NEEDED, RESOURCE_CRN);
    }
}