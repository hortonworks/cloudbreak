package com.sequenceiq.freeipa.service.crossrealm;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class TrustSetupServiceTest {
    @InjectMocks
    private TrustSetupService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private TrustCommandsGeneratorService trustCommandsGeneratorService;

    @Test
    void returnsTrustSetupCommandsResponseWhenStatusIsAllowed() {
        String accountId = "acc";
        String envCrn = "env-crn";
        TrustSetupCommandsRequest request = new TrustSetupCommandsRequest();
        request.setEnvironmentCrn(envCrn);

        Stack stack = mock(Stack.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        TrustSetupCommandsResponse expectedResponse = mock(TrustSetupCommandsResponse.class);
        StackStatus stackStatus = mock(StackStatus.class);

        when(stackService.getFreeIpaStackWithMdcContext(envCrn, accountId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stack.getId())).thenReturn(crossRealmTrust);
        when(crossRealmTrust.getTrustStatus()).thenReturn(TrustStatus.TRUST_ACTIVE);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(trustCommandsGeneratorService.getTrustSetupCommands(request, stack, freeIpa, crossRealmTrust)).thenReturn(expectedResponse);

        TrustSetupCommandsResponse response = underTest.getTrustSetupCommands(accountId, request);

        assertSame(expectedResponse, response);
    }

    @Test
    void throwsBadRequestExceptionWhenTrustStatusIsNotAllowed() {
        String accountId = "acc";
        String envCrn = "env-crn";
        TrustSetupCommandsRequest request = new TrustSetupCommandsRequest();
        request.setEnvironmentCrn(envCrn);

        Stack stack = mock(Stack.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);
        StackStatus stackStatus = mock(StackStatus.class);

        when(stackService.getFreeIpaStackWithMdcContext(envCrn, accountId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stack.getId())).thenReturn(crossRealmTrust);
        when(crossRealmTrust.getTrustStatus()).thenReturn(TrustStatus.TRUST_SETUP_REQUIRED);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        when(stackStatus.getDetailedStackStatus()).thenReturn(DetailedStackStatus.AVAILABLE);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> underTest.getTrustSetupCommands(accountId, request));
        assertTrue(ex.getMessage().contains("trust is not in state, where trust setup commands can be generated"));
    }
}