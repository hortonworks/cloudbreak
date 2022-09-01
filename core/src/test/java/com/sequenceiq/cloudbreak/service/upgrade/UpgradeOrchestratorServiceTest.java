package com.sequenceiq.cloudbreak.service.upgrade;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;

@ExtendWith(MockitoExtension.class)
public class UpgradeOrchestratorServiceTest {

    private static final long STACK_ID = 123L;

    @Mock
    private ClusterServiceRunner clusterServiceRunner;

    @InjectMocks
    private UpgradeOrchestratorService underTest;

    @Test
    void testPushSaltStates() {
        underTest.pushSaltState(STACK_ID);

        verify(clusterServiceRunner).redeployStates(STACK_ID);
        verify(clusterServiceRunner).redeployGatewayPillar(STACK_ID);
    }
}
