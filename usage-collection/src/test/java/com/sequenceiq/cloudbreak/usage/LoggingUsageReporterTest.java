package com.sequenceiq.cloudbreak.usage;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class LoggingUsageReporterTest {
    LoggingUsageReporter usageReporter = spy(LoggingUsageReporter.class);

    String dummy = "dummy";

    @Test
    public void cdpDatahubClusterRequested() {
        long timestamp = System.currentTimeMillis();
        UsageProto.CDPDatahubClusterRequested proto = UsageProto.CDPDatahubClusterRequested.newBuilder()
                .setAccountId(dummy)
                .setCdpdVersion(dummy)
                .setClusterId(dummy)
                .setClusterName(dummy)
                .setCreatorCrn(dummy)
                .setDatalakeCrn(dummy)
                .build();
        usageReporter.cdpDatahubClusterRequested(timestamp, proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(usageReporter).log(captor.capture());

        assertEquals(timestamp, captor.getValue().getTimestamp());
        assertEquals(dummy, captor.getValue().getCdpDatahubClusterRequested().getCreatorCrn());
    }

    @Test
    public void cdpDatahubClusterStatusChanged() {
        UsageProto.CDPDatahubClusterStatusChanged proto = UsageProto.CDPDatahubClusterStatusChanged.newBuilder()
                .setClusterId(dummy)
                .setOldStatus(UsageProto.CDPCloudbreakClusterStatus.Value.REQUESTED)
                .setNewStatus(UsageProto.CDPCloudbreakClusterStatus.Value.CREATE_IN_PROGRESS)
                .build();
        usageReporter.cdpDatahubClusterStatusChanged(proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(usageReporter).log(captor.capture());

        assertEquals(dummy, captor.getValue().getCdpDatahubClusterStatusChanged().getClusterId());
    }

    @Test
    public void cdpDatalakeClusterRequested() {
        long timestamp = System.currentTimeMillis();
        UsageProto.CDPDatalakeClusterRequested proto = UsageProto.CDPDatalakeClusterRequested.newBuilder()
                .setAccountId(dummy)
                .setCdpdVersion(dummy)
                .setDatalakeId(dummy)
                .setDatalakeName(dummy)
                .setCreatorCrn(dummy)
                .build();
        usageReporter.cdpDatalakeClusterRequested(timestamp, proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(usageReporter).log(captor.capture());

        assertEquals(timestamp, captor.getValue().getTimestamp());
        assertEquals(dummy, captor.getValue().getCdpDatalakeClusterRequested().getCreatorCrn());
    }

    @Test
    public void cdpDatalakeClusterStatusChanged() {
        UsageProto.CDPDatalakeClusterStatusChanged proto = UsageProto.CDPDatalakeClusterStatusChanged.newBuilder()
                .setDatalakeId(dummy)
                .setOldStatus(UsageProto.CDPCloudbreakClusterStatus.Value.REQUESTED)
                .setNewStatus(UsageProto.CDPCloudbreakClusterStatus.Value.CREATE_IN_PROGRESS)
                .build();
        usageReporter.cdpDatalakeClusterStatusChanged(proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(usageReporter).log(captor.capture());

        assertEquals(dummy, captor.getValue().getCdpDatalakeClusterStatusChanged().getDatalakeId());
    }
}