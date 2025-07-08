package com.sequenceiq.freeipa.service.crossrealm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsRequest;
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

    @InjectMocks
    private TrustCommandsGeneratorService underTest;

    @Test
    void returnsCommandsResponseWithExpectedFields() {
        TrustSetupCommandsRequest request = new TrustSetupCommandsRequest();
        request.setEnvironmentCrn("env-crn");
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);

        when(activeDirectoryCommandsBuilder.buildCommands(stack, freeIpa, crossRealmTrust)).thenReturn("active directory commands");
        when(baseClusterKrb5ConfBuilder.buildCommands(freeIpa, crossRealmTrust)).thenReturn("krb5 conf");

        TrustSetupCommandsResponse response = underTest.getTrustSetupCommands(request, stack, freeIpa, crossRealmTrust);

        assertEquals("env-crn", response.getEnvironmentCrn());
        assertEquals("active directory commands", response.getActiveDirectoryCommands().getCommands());
        assertEquals("krb5 conf", response.getBaseClusterCommands().getKrb5Conf());
    }

}