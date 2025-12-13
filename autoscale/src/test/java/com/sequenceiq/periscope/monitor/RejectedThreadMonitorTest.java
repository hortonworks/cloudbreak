package com.sequenceiq.periscope.monitor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.service.RejectedThreadService;

@ExtendWith(MockitoExtension.class)
public class RejectedThreadMonitorTest {

    @InjectMocks
    private final RejectedThreadMonitor underTest = new RejectedThreadMonitor();

    @Mock
    private RejectedThreadService rejectedThreadService;

    @Test
    public void testGetMonitored() {

        when(rejectedThreadService.getAllRejectedCluster())
                .thenReturn(Arrays.asList(rejectedThread(3), rejectedThread(2),
                        rejectedThread(6), rejectedThread(4),
                        rejectedThread(10), rejectedThread(6)));

        List<RejectedThread> monitored = underTest.getMonitored();

        Long[] collect = new Long[6];
        monitored.stream().map(RejectedThread::getRejectedCount).collect(Collectors.toList()).toArray(collect);
        assertArrayEquals(collect, new Long[]{10L, 6L, 6L, 4L, 3L, 2L});
    }

    private RejectedThread rejectedThread(long count) {
        RejectedThread rejectedThread = new RejectedThread();
        rejectedThread.setRejectedCount(count);
        return rejectedThread;

    }
}
