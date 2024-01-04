package com.sequenceiq.freeipa.service.stack.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.metrics.status.StackCountByStatusView;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.service.stack.StackStatusService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;

@ExtendWith(MockitoExtension.class)
class StackStatusMetricCollectorTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private StackStatusService stackStatusService;

    @InjectMocks
    private StackStatusMetricCollector underTest;

    @Mock
    private MultiGauge stackStatusByCloudPlatformMultiGauge;

    @Mock
    private MultiGauge stackStatusByTunnelMultiGauge;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(underTest, "stackStatusByCloudPlatformMultiGauge", stackStatusByCloudPlatformMultiGauge);
        ReflectionTestUtils.setField(underTest, "stackStatusByTunnelMultiGauge", stackStatusByTunnelMultiGauge);
    }

    @Test
    void testCollectStatusMetric() {
        when(stackStatusService.countStacksByStatusAndCloudPlatform(eq("AWS")))
                .thenReturn(List.of(stackCountByStatus(3, Status.AVAILABLE), stackCountByStatus(2, Status.STOPPED)));
        when(stackStatusService.countStacksByStatusAndCloudPlatform(eq("AZURE")))
                .thenReturn(List.of(stackCountByStatus(1, Status.AVAILABLE), stackCountByStatus(4, Status.STOPPED),
                        stackCountByStatus(5, Status.UPDATE_FAILED)));
        when(stackStatusService.countStacksByStatusAndCloudPlatform(eq("GCP"))).thenReturn(List.of());

        lenient().when(stackStatusService.countStacksByStatusAndTunnel(eq(Tunnel.DIRECT)))
                .thenReturn(List.of(stackCountByStatus(2, Status.AVAILABLE), stackCountByStatus(6, Status.STOPPED)));
        lenient().when(stackStatusService.countStacksByStatusAndTunnel(eq(Tunnel.CCMV2_JUMPGATE)))
                .thenReturn(List.of(stackCountByStatus(1, Status.AVAILABLE), stackCountByStatus(1, Status.DELETE_FAILED)));
        lenient().when(stackStatusService.countStacksByStatusAndTunnel(eq(Tunnel.CCM))).thenReturn(List.of());
        lenient().when(stackStatusService.countStacksByStatusAndTunnel(eq(Tunnel.CCMV2))).thenReturn(List.of());

        underTest.collectStatusMetrics();

        ArgumentCaptor<List<MultiGauge.Row<?>>> statusByCloudPlatformRowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stackStatusByCloudPlatformMultiGauge, times(1)).register(statusByCloudPlatformRowsCaptor.capture(), eq(Boolean.TRUE));
        List<MultiGauge.Row<?>> statusByCloudProviderRows = statusByCloudPlatformRowsCaptor.getValue();
        assertEquals(5, statusByCloudProviderRows.size());

        ArgumentCaptor<List<MultiGauge.Row<?>>> statusByTunnelRowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stackStatusByTunnelMultiGauge, times(1)).register(statusByTunnelRowsCaptor.capture(), eq(Boolean.TRUE));
        List<MultiGauge.Row<?>> statusByTunnelRows = statusByTunnelRowsCaptor.getValue();
        assertEquals(4, statusByTunnelRows.size());
    }

    private StackCountByStatusView stackCountByStatus(int count, Status status) {
        return new StackCountByStatusView() {
            @Override
            public String getStatus() {
                return status.name();
            }

            @Override
            public int getCount() {
                return count;
            }
        };
    }
}