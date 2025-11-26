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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.MitBaseClusterKrb5ConfBuilder;
import com.sequenceiq.freeipa.service.crossrealm.MitDnsInstructionsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.MitKdcCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;

@ExtendWith(MockitoExtension.class)
class MitKdcTrustServiceTest {

    @Mock
    private MitBaseClusterKrb5ConfBuilder mitBaseClusterKrb5ConfBuilder;

    @Mock
    private MitKdcCommandsBuilder mitKdcCommandsBuilder;

    @Mock
    private MitDnsInstructionsBuilder mitDnsInstructionsBuilder;

    @InjectMocks
    private MitKdcTrustService underTest;

    @Test
    void returnsCommandsResponseWithMitExpectedFields() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);

        when(mitKdcCommandsBuilder.buildCommands(TrustCommandType.SETUP, freeIpa, crossRealmTrust)).thenReturn("mit commands");
        when(mitDnsInstructionsBuilder.buildCommands(TrustCommandType.SETUP, stack, freeIpa)).thenReturn("dns instructions");
        when(mitBaseClusterKrb5ConfBuilder.buildCommands(stack.getResourceName(), TrustCommandType.SETUP, freeIpa, crossRealmTrust, loadBalancer))
                .thenReturn("krb5 conf");

        TrustSetupCommandsResponse response = underTest.buildTrustSetupCommandsResponse(TrustCommandType.SETUP, "env-crn", stack, freeIpa,
                crossRealmTrust, loadBalancer);

        assertEquals("env-crn", response.getEnvironmentCrn());
        assertEquals(KdcType.MIT.name(), response.getKdcType());
        assertNull(response.getActiveDirectoryCommands());
        assertEquals("mit commands", response.getMitCommands().getKdcCommands());
        assertEquals("dns instructions", response.getMitCommands().getDnsSetupInstructions());
        assertEquals("krb5 conf", response.getBaseClusterCommands().getKrb5Conf());
    }
}