package com.sequenceiq.freeipa.service.crossrealm.commands.mit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.MitTrustSetupCommands;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@ExtendWith(MockitoExtension.class)
class MitTrustInstructionsBuilderTest {
    @Mock
    private MitBaseClusterKrb5ConfBuilder mitBaseClusterKrb5ConfBuilder;

    @Mock
    private MitKdcCommandsBuilder mitKdcCommandsBuilder;

    @Mock
    private MitDnsInstructionsBuilder mitDnsInstructionsBuilder;

    @InjectMocks
    private MitTrustInstructionsBuilder underTest;

    @Test
    void testBuildInstructions() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);

        when(mitKdcCommandsBuilder.buildCommands(TrustCommandType.SETUP, freeIpa, crossRealmTrust)).thenReturn("mit commands");
        when(mitDnsInstructionsBuilder.buildCommands(TrustCommandType.SETUP, stack, freeIpa)).thenReturn("dns instructions");

        MitTrustSetupCommands actualResult = underTest.buildInstructions(TrustCommandType.SETUP, stack, freeIpa, crossRealmTrust);

        assertEquals("mit commands", actualResult.getKdcCommands());
        assertEquals("dns instructions", actualResult.getDnsSetupInstructions());
    }
}
