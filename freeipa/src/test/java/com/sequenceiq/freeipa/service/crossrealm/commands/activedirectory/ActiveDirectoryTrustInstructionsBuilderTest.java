package com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ActiveDirectoryTrustSetupCommands;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@ExtendWith(MockitoExtension.class)
class ActiveDirectoryTrustInstructionsBuilderTest {
    @Mock
    private ActiveDirectoryKdcCommandsBuilder activeDirectoryKdcCommandsBuilder;

    @InjectMocks
    private ActiveDirectoryTrustInstructionsBuilder underTest;

    @Test
    void testBuildInstructions() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);

        when(activeDirectoryKdcCommandsBuilder.buildCommands(TrustCommandType.SETUP, stack, freeIpa, crossRealmTrust)).thenReturn("active directory commands");

        ActiveDirectoryTrustSetupCommands actualResult = underTest.buildInstructions(TrustCommandType.SETUP, stack, freeIpa, crossRealmTrust);

        Assertions.assertEquals("active directory commands", actualResult.getCommands());
    }

    @Test
    void testValidationInstructions() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);

        when(activeDirectoryKdcCommandsBuilder.buildCommands(TrustCommandType.VALIDATION, stack, freeIpa, crossRealmTrust))
                .thenReturn("active directory validation commands");

        ActiveDirectoryTrustSetupCommands actualResult = underTest.buildInstructions(TrustCommandType.VALIDATION, stack, freeIpa, crossRealmTrust);

        Assertions.assertEquals("active directory validation commands", actualResult.getCommands());
    }
}
