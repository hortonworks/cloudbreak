package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.freeipa;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.CDPFreeipaStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class CDPFreeIpaSyncLoggerTest {

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private CDPFreeipaStructuredSyncEventToCDPFreeIPASyncConverter freeipaConverter;

    @InjectMocks
    private CDPFreeIpaSyncLogger underTest;

    @Test
    void testLog() {
        CDPFreeipaStructuredSyncEvent cdpFreeipaStructuredSyncEvent = mock(CDPFreeipaStructuredSyncEvent.class);
        UsageProto.CDPFreeIPASync cdpFreeIPASync = mock(UsageProto.CDPFreeIPASync.class);
        when(freeipaConverter.convert(cdpFreeipaStructuredSyncEvent)).thenReturn(cdpFreeIPASync);

        underTest.log(cdpFreeipaStructuredSyncEvent);

        verify(usageReporter).cdpFreeipaSync(cdpFreeIPASync);
    }

}
