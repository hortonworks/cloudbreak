package com.sequenceiq.freeipa.service.crossrealm.commands.mit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@ExtendWith(MockitoExtension.class)
class MitBaseClusterTrustCommandsBuilderTest {
    @Mock
    private MitBaseClusterKrb5ConfBuilder mitBaseClusterKrb5ConfBuilder;

    @InjectMocks
    private MitBaseClusterTrustCommandsBuilder underTest;

    @Test
    void testBuildKrb5Conf() {
        when(mitBaseClusterKrb5ConfBuilder.buildCommands(any(), any(), any(), any(), any())).thenReturn("command");

        String actualResult = underTest.buildKrb5Conf("resource", TrustCommandType.CLEANUP, new FreeIpa(), new CrossRealmTrust(), new LoadBalancer());

        assertEquals("command", actualResult);
    }
}
