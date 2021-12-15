package com.sequenceiq.cloudbreak.usage;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.strategy.CloudwatchUsageProcessingStrategy;
import com.sequenceiq.cloudbreak.usage.strategy.LoggingUsageProcessingStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UsageReportProcessorTest {

    private static final String DUMMY = "dummy";

    private UsageReportProcessor usageReporter;

    @Mock
    private LoggingUsageProcessingStrategy loggingUsageProcessingStrategy;

    @Mock
    private CloudwatchUsageProcessingStrategy cloudwatchUsageProcessingStrategy;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        usageReporter = new UsageReportProcessor(loggingUsageProcessingStrategy, cloudwatchUsageProcessingStrategy);
    }

    @Test
    public void cdpDatahubClusterRequested() {
        long timestamp = System.currentTimeMillis();
        UsageProto.CDPDatahubClusterRequested proto = UsageProto.CDPDatahubClusterRequested.newBuilder()
                .setAccountId(DUMMY)
                .setCdpdVersion(DUMMY)
                .setClusterId(DUMMY)
                .setClusterName(DUMMY)
                .setCreatorCrn(DUMMY)
                .setDatalakeCrn(DUMMY)
                .build();
        usageReporter.cdpDatahubClusterRequested(timestamp, proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(loggingUsageProcessingStrategy).processUsage(captor.capture());

        assertEquals(timestamp, captor.getValue().getTimestamp());
        assertEquals(DUMMY, captor.getValue().getCdpDatahubClusterRequested().getCreatorCrn());
    }

    @Test
    public void cdpDatahubClusterStatusChanged() {
        UsageProto.CDPDatahubClusterStatusChanged proto = UsageProto.CDPDatahubClusterStatusChanged.newBuilder()
                .setClusterId(DUMMY)
                .setOldStatus(UsageProto.CDPCloudbreakClusterStatus.Value.REQUESTED)
                .setNewStatus(UsageProto.CDPCloudbreakClusterStatus.Value.CREATE_IN_PROGRESS)
                .build();
        usageReporter.cdpDatahubClusterStatusChanged(proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(loggingUsageProcessingStrategy).processUsage(captor.capture());

        assertEquals(DUMMY, captor.getValue().getCdpDatahubClusterStatusChanged().getClusterId());
    }

    @Test
    public void cdpDatalakeClusterRequested() {
        long timestamp = System.currentTimeMillis();
        UsageProto.CDPDatalakeClusterRequested proto = UsageProto.CDPDatalakeClusterRequested.newBuilder()
                .setAccountId(DUMMY)
                .setCdpdVersion(DUMMY)
                .setDatalakeId(DUMMY)
                .setDatalakeName(DUMMY)
                .setCreatorCrn(DUMMY)
                .build();
        usageReporter.cdpDatalakeClusterRequested(timestamp, proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(loggingUsageProcessingStrategy).processUsage(captor.capture());

        assertEquals(timestamp, captor.getValue().getTimestamp());
        assertEquals(DUMMY, captor.getValue().getCdpDatalakeClusterRequested().getCreatorCrn());
    }

    @Test
    public void cdpDatalakeClusterStatusChanged() {
        UsageProto.CDPDatalakeClusterStatusChanged proto = UsageProto.CDPDatalakeClusterStatusChanged.newBuilder()
                .setDatalakeId(DUMMY)
                .setOldStatus(UsageProto.CDPCloudbreakClusterStatus.Value.REQUESTED)
                .setNewStatus(UsageProto.CDPCloudbreakClusterStatus.Value.CREATE_IN_PROGRESS)
                .build();
        usageReporter.cdpDatalakeClusterStatusChanged(proto);

        ArgumentCaptor<UsageProto.Event> captor = ArgumentCaptor.forClass(UsageProto.Event.class);
        verify(loggingUsageProcessingStrategy).processUsage(captor.capture());

        assertEquals(DUMMY, captor.getValue().getCdpDatalakeClusterStatusChanged().getDatalakeId());
    }
}