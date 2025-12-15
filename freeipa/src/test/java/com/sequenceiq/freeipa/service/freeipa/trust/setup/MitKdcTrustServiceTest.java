package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.MitTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.commands.mit.MitBaseClusterTrustCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.commands.mit.MitTrustInstructionsBuilder;

@ExtendWith(MockitoExtension.class)
class MitKdcTrustServiceTest {
    @Mock
    private MitTrustInstructionsBuilder mitTrustInstructionsBuilder;

    @Mock
    private MitBaseClusterTrustCommandsBuilder mitBaseClusterTrustCommandsBuilder;

    @InjectMocks
    private MitKdcTrustService underTest;

    @Test
    void returnsCommandsResponseWithMitExpectedFields() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);
        MitTrustSetupCommands mitTrustSetupCommands = new MitTrustSetupCommands();
        BaseClusterTrustSetupCommands baseClusterTrustSetupCommands = new BaseClusterTrustSetupCommands();

        when(mitTrustInstructionsBuilder.buildInstructions(TrustCommandType.SETUP, stack, freeIpa, crossRealmTrust)).thenReturn(mitTrustSetupCommands);
        when(mitBaseClusterTrustCommandsBuilder.buildBaseClusterCommands(stack, TrustCommandType.SETUP, freeIpa, crossRealmTrust, loadBalancer))
                .thenReturn(baseClusterTrustSetupCommands);

        TrustSetupCommandsResponse response = underTest.buildTrustSetupCommandsResponse(TrustCommandType.SETUP, "env-crn", stack, freeIpa,
                crossRealmTrust, loadBalancer);

        assertEquals("env-crn", response.getEnvironmentCrn());
        assertEquals(KdcType.MIT.name(), response.getKdcType());
        assertEquals(mitTrustSetupCommands, response.getMitCommands());
        assertEquals(baseClusterTrustSetupCommands, response.getBaseClusterCommands());
        assertNull(response.getActiveDirectoryCommands());
    }
}
