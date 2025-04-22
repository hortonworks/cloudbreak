package com.sequenceiq.freeipa.service.rotation.dbuscredential.contextprovider;


import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaDbusUmsAccessKeyRotationContextProviderTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:12474ddc-6e44-4f4c-806a-b197ef12cbb8";

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Mock
    private SecretRotationSaltService saltService;

    @InjectMocks
    private FreeIpaDbusUmsAccessKeyRotationContextProvider underTest;

    @Mock
    private Telemetry telemetry;

    @Mock
    private Stack stack;

    @BeforeEach
    void setup() {
        lenient().when(entitlementService.useDataBusCNameEndpointEnabled(any())).thenReturn(Boolean.TRUE);
        lenient().when(dataBusEndpointProvider.getDataBusEndpoint(any(), anyBoolean())).thenReturn("");
        lenient().when(stackService.getByEnvironmentCrnAndAccountIdWithLists(any(), any())).thenReturn(stack);
        lenient().when(stack.getTelemetry()).thenReturn(telemetry);
        lenient().when(stack.getDatabusCredential()).thenReturn("{\"privateKey\":\"anything\"}");
    }

    @Test
    void testGetContexts() {
        assertEquals(2, underTest.getContexts(ENV_CRN).size());
    }

    @Test
    void testCustomJobRotateDatabusCredReadFailure() {
        when(stack.getDatabusCredential()).thenReturn("invalidjson");
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts(ENV_CRN).get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to read Databus credential from internal database.");

        verifyNoInteractions(saltService);
    }

    @Test
    void testCustomJobRotateRefreshPillarFailure() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("failed")).when(saltService).updateSaltPillar(any(), any());
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts(ENV_CRN).get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to refresh Databus relevant salt pillars.");

        verify(saltService, times(1)).updateSaltPillar(any(), any());
        verify(saltService, never()).executeSaltState(any(), any(), any());
    }

    @Test
    void testCustomJobRotateExecuteSaltStateFailure() throws CloudbreakOrchestratorFailedException {
        doNothing().when(saltService).updateSaltPillar(any(), any());
        when(stack.getAllNotDeletedNodes()).thenReturn(Set.of());
        doThrow(new CloudbreakOrchestratorFailedException("failed")).when(saltService).executeSaltState(any(), any(), any());
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts(ENV_CRN).get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertThrows(SecretRotationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run()),
                "Failed to execute Databus relevant salt states.");

        verify(saltService, times(1)).updateSaltPillar(any(), any());
        verify(saltService, times(1)).executeSaltState(any(), any(), any());
    }

    @Test
    void testCustomJobRotateAndRollbackSuccess() throws Exception {
        doNothing().when(saltService).updateSaltPillar(any(), any());
        when(stack.getAllNotDeletedNodes()).thenReturn(Set.of());
        doNothing().when(saltService).executeSaltState(any(), any(), any());
        CustomJobRotationContext customJobRotationContext = (CustomJobRotationContext) underTest.getContexts(ENV_CRN).get(CUSTOM_JOB);
        assertTrue(customJobRotationContext.getRotationJob().isPresent());
        assertTrue(customJobRotationContext.getRollbackJob().isPresent());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRotationJob().get().run());
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> customJobRotationContext.getRollbackJob().get().run());

        verify(saltService, times(2)).updateSaltPillar(any(), any());
        verify(saltService, times(2)).executeSaltState(any(), any(), any());
    }
}
