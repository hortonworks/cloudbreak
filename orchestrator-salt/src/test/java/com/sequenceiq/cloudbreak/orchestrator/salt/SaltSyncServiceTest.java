package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@ExtendWith(MockitoExtension.class)
public class SaltSyncServiceTest {

    @Mock
    private SaltStateService saltStateService;

    @Mock
    private SaltService saltService;

    @InjectMocks
    private SaltSyncService underTest;

    @Test
    public void testCheckMinionsIfOk() {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        MinionStatusSaltResponse response = new MinionStatusSaltResponse();
        response.setResult(List.of());
        when(saltStateService.collectNodeStatusWithLimitedRetry(any())).thenReturn(response);

        Optional<Set<String>> failedMinions =
                underTest.checkSaltMinions(new GatewayConfig(null, null, null, null, null, null));

        assertFalse(failedMinions.isPresent());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any());
    }

    @Test
    public void testCheckMinionsIfNok() {
        SaltConnector saltConnector = mock(SaltConnector.class);
        when(saltService.createSaltConnectorWithCustomTimeout(any(), anyInt(), anyInt(), anyInt())).thenReturn(saltConnector);
        MinionStatusSaltResponse response = new MinionStatusSaltResponse();
        MinionStatus minionStatus = new MinionStatus();
        minionStatus.setDown(List.of("host"));
        response.setResult(List.of(minionStatus));
        when(saltStateService.collectNodeStatusWithLimitedRetry(any())).thenReturn(response);

        Optional<Set<String>> failedMinions =
                underTest.checkSaltMinions(new GatewayConfig(null, null, null, null, null, null));

        assertTrue(failedMinions.isPresent());
        verify(saltStateService).collectNodeStatusWithLimitedRetry(any());
    }
}
