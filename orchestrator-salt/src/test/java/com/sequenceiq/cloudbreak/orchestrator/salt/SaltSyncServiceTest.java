package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusFromFileResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusWithTimestamp;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
public class SaltSyncServiceTest {

    private static final String HOST_1 = "host1";

    private static final String HOST_2 = "host2";

    private static final GatewayConfig GW = new GatewayConfig(null, null, null, HOST_1, null, null, null, null, null, null, null, null, Boolean.TRUE, true,
            null, null, null, null, null, null, null, null);

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltService saltService;

    @InjectMocks
    private SaltSyncService underTest;

    @Test
    public void testCheckMinionsIfOk() throws JsonProcessingException {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltConnector.getHostname()).thenReturn(HOST_1);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        MinionStatusFromFileResponse response = new MinionStatusFromFileResponse();
        MinionStatusWithTimestamp status = new MinionStatusWithTimestamp();
        status.setTimestamp(System.currentTimeMillis());
        response.setResult(List.of(Map.of(HOST_1, JsonUtil.writeValueAsString(status))));
        when(saltStateService.collectNodeStatusWithLimitedRetry(any(), any())).thenReturn(response);

        Optional<Set<String>> failedMinions = underTest.checkSaltMinions(GW);

        assertFalse(failedMinions.isPresent());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any(), any());
    }

    @Test
    public void testCheckMinionsIfNok() throws JsonProcessingException {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltConnector.getHostname()).thenReturn(HOST_1);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        MinionStatusFromFileResponse response = new MinionStatusFromFileResponse();
        MinionStatusWithTimestamp status = new MinionStatusWithTimestamp();
        status.setTimestamp(System.currentTimeMillis());
        status.setDown(List.of(HOST_2));
        response.setResult(List.of(Map.of(HOST_1, JsonUtil.writeValueAsString(status))));
        when(saltStateService.collectNodeStatusWithLimitedRetry(any(), any())).thenReturn(response);

        Optional<Set<String>> failedMinions = underTest.checkSaltMinions(GW);

        assertTrue(failedMinions.isPresent());
        assertEquals(HOST_2, failedMinions.get().iterator().next());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any(), any());
    }

    @Test
    public void testCheckMinionsIfResultJsonInvalid() throws JsonProcessingException {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltConnector.getHostname()).thenReturn(HOST_1);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        MinionStatusFromFileResponse response = new MinionStatusFromFileResponse();
        response.setResult(List.of(Map.of(HOST_1, JsonUtil.writeValueAsString("anything"))));
        when(saltStateService.collectNodeStatusWithLimitedRetry(any(), any())).thenReturn(response);

        Optional<Set<String>> failedMinions = underTest.checkSaltMinions(GW);

        assertFalse(failedMinions.isPresent());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any(), any());
    }

    @Test
    public void testCheckMinionsIfResultJsonOld() throws JsonProcessingException {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltConnector.getHostname()).thenReturn(HOST_1);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        MinionStatusFromFileResponse response = new MinionStatusFromFileResponse();
        MinionStatusWithTimestamp status = new MinionStatusWithTimestamp();
        status.setTimestamp(Instant.now().minusSeconds(Duration.ofMinutes(15).toSeconds()).getEpochSecond());
        status.setDown(List.of(HOST_2));
        response.setResult(List.of(Map.of(HOST_1, JsonUtil.writeValueAsString(status))));
        when(saltStateService.collectNodeStatusWithLimitedRetry(any(), any())).thenReturn(response);

        Optional<Set<String>> failedMinions = underTest.checkSaltMinions(GW);

        assertFalse(failedMinions.isPresent());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any(), any());
    }

    @Test
    public void testCheckMinionsIfConnectorFails() throws JsonProcessingException {
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("anything"));

        Optional<Set<String>> failedMinions = underTest.checkSaltMinions(GW);

        assertTrue(failedMinions.isPresent());
        assertEquals(HOST_1, failedMinions.get().iterator().next());
        verify(saltStateService, never()).collectNodeStatusWithLimitedRetry(any(), any());
    }

    @Test
    public void testCheckMinionsIfSaltCallFails() throws JsonProcessingException {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        when(saltStateService.collectNodeStatusWithLimitedRetry(any(), any())).thenThrow(new RuntimeException("anything"));

        Optional<Set<String>> failedMinions = underTest.checkSaltMinions(GW);

        assertTrue(failedMinions.isPresent());
        assertEquals(HOST_1, failedMinions.get().iterator().next());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any(), any());
    }
}
