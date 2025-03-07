package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.environment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredSyncEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class CDPEnvironmentSyncLoggerTest {

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private CDPEnvironmentStructuredSyncEventToCDPEnvironmentSyncConverter environmentConverter;

    @InjectMocks
    private CDPEnvironmentSyncLogger underTest;

    @Test
    void testLog() {
        CDPEnvironmentStructuredSyncEvent cdpEnvironmentStructuredSyncEvent = mock(CDPEnvironmentStructuredSyncEvent.class);
        UsageProto.CDPEnvironmentSync cdpEnvironmentSync = mock(UsageProto.CDPEnvironmentSync.class);
        when(environmentConverter.convert(cdpEnvironmentStructuredSyncEvent)).thenReturn(cdpEnvironmentSync);

        underTest.log(cdpEnvironmentStructuredSyncEvent);

        verify(usageReporter).cdpEnvironmentSync(cdpEnvironmentSync);
    }
}
