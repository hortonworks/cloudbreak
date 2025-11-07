package com.sequenceiq.freeipa.service.crossrealm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class TrustCommandsGeneratorServiceTest {
    @Mock
    private ActiveDirectoryCommandsBuilder activeDirectoryCommandsBuilder;

    @Mock
    private BaseClusterKrb5ConfBuilder baseClusterKrb5ConfBuilder;

    @Mock
    private MitKdcCommandsBuilder mitKdcCommandsBuilder;

    @Mock
    private MitDnsInstructionsBuilder mitDnsInstructionsBuilder;

    @InjectMocks
    private TrustCommandsGeneratorService underTest;

    @Test
    void returnsCommandsResponseWithADExpectedFields() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);
        when(crossRealmTrust.getKdcType()).thenReturn(KdcType.ACTIVE_DIRECTORY);

        when(activeDirectoryCommandsBuilder.buildCommands(TrustCommandType.SETUP, stack, freeIpa, crossRealmTrust)).thenReturn("active directory commands");
        when(baseClusterKrb5ConfBuilder.buildCommands(TrustCommandType.SETUP, freeIpa, crossRealmTrust)).thenReturn("krb5 conf");

        TrustSetupCommandsResponse response = underTest.getTrustCommands(TrustCommandType.SETUP, "env-crn", stack, freeIpa, crossRealmTrust);

        assertEquals("env-crn", response.getEnvironmentCrn());
        assertEquals(KdcType.ACTIVE_DIRECTORY.name(), response.getKdcType());
        assertEquals("active directory commands", response.getActiveDirectoryCommands().getCommands());
        assertEquals("krb5 conf", response.getBaseClusterCommands().getKrb5Conf());
    }

    @Test
    void returnsCommandsResponseWithMitExpectedFields() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);
        when(crossRealmTrust.getKdcType()).thenReturn(KdcType.MIT);

        when(mitKdcCommandsBuilder.buildCommands(TrustCommandType.SETUP, freeIpa, crossRealmTrust)).thenReturn("mit commands");
        when(mitDnsInstructionsBuilder.buildCommands(TrustCommandType.SETUP, stack, freeIpa)).thenReturn("dns instructions");
        when(baseClusterKrb5ConfBuilder.buildCommands(TrustCommandType.SETUP, freeIpa, crossRealmTrust)).thenReturn("krb5 conf");

        TrustSetupCommandsResponse response = underTest.getTrustCommands(TrustCommandType.SETUP, "env-crn", stack, freeIpa, crossRealmTrust);

        assertEquals("env-crn", response.getEnvironmentCrn());
        assertEquals(KdcType.MIT.name(), response.getKdcType());
        assertNull(response.getActiveDirectoryCommands());
        assertEquals("mit commands", response.getMitCommands().getKdcCommands());
        assertEquals("dns instructions", response.getMitCommands().getDnsSetupInstructions());
        assertEquals("krb5 conf", response.getBaseClusterCommands().getKrb5Conf());
    }

    @Test
    void returnsCommandsResponseWithoutKdcType() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);

        assertThrows(BadRequestException.class, () -> underTest.getTrustCommands(TrustCommandType.SETUP, "env-crn", stack, freeIpa, crossRealmTrust));
    }
}
