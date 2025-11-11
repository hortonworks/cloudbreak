package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.Trust;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

@ExtendWith(MockitoExtension.class)
class ActiveDirectoryTrustServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private TrustStatusValidationService trustStatusValidationService;

    @Mock
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @InjectMocks
    private ActiveDirectoryTrustService underTest;

    @Mock
    private Stack stack;

    @Mock
    private CrossRealmTrust crossRealmTrust;

    @Mock
    private FreeIpaClient client;

    @Mock
    private ValidationResult validationResult;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void successWithActiveDirectoryKdc() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("secret");
        crossRealmTrust.setKdcRealm("hybrid.cloudera.org");
        when(crossRealmTrustService.getByStackId(STACK_ID)).thenReturn(crossRealmTrust);
        lenient().doReturn(client).when(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        when(trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust)).thenReturn(validationResult);
        when(validationResult.hasError()).thenReturn(false);

        underTest.addTrust(STACK_ID);
        underTest.validateTrust(STACK_ID);

        verify(client).addTrust("secret", "ad", true, "HYBRID.CLOUDERA.ORG");
        verify(trustStatusValidationService).validateTrustStatus(stack, crossRealmTrust);
    }

    @Test
    void validationFailure() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("secret");
        crossRealmTrust.setKdcRealm("hybrid.cloudera.org");
        when(crossRealmTrustService.getByStackId(STACK_ID)).thenReturn(crossRealmTrust);
        lenient().doReturn(client).when(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        when(trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust)).thenReturn(validationResult);
        when(validationResult.hasError()).thenReturn(true);
        when(validationResult.getFormattedErrors()).thenReturn("errors");

        assertThatThrownBy(() -> underTest.validateTrust(STACK_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to validate trust on FreeIPA: errors");
    }

    @Test
    void testKdcType() {
        KdcType type = underTest.kdcType();
        Assertions.assertEquals(KdcType.ACTIVE_DIRECTORY, type);
    }

    @Test
    void testPrepare() throws Exception {
        Long stackId = 1L;
        ArgumentCaptor<OrchestratorStateParams> params = ArgumentCaptor.forClass(OrchestratorStateParams.class);

        doReturn(stack).when(underTest.getStackService()).getByIdWithListsInTransaction(stackId);
        OrchestratorStateParams mockParams = mock(OrchestratorStateParams.class);
        when(mockParams.getState()).thenReturn("trustsetup.adtrust_install");
        when(saltStateParamsService.createStateParams(stack, "trustsetup.adtrust_install", false, 0, 0))
                .thenReturn(mockParams);

        underTest.prepare(stackId);

        verify(underTest.getHostOrchestrator()).runOrchestratorState(params.capture());
        OrchestratorStateParams capturedParams = params.getValue();
        Assertions.assertEquals("trustsetup.adtrust_install", capturedParams.getState());
    }

    @Test
    void testAddTrust() throws Exception {
        Long stackId = 2L;
        Trust trust = mock(Trust.class);

        doReturn(stack).when(underTest.getStackService()).getByIdWithListsInTransaction(stackId);
        doReturn(crossRealmTrust).when(underTest.getCrossRealmTrustService()).getByStackId(stackId);
        doReturn(client).when(underTest.getFreeIpaClientFactory()).getFreeIpaClientForStack(stack);
        when(crossRealmTrust.getTrustSecret()).thenReturn("secret");
        when(crossRealmTrust.getKdcRealm()).thenReturn("realm");
        when(client.addTrust("secret", "ad", true, "REALM")).thenReturn(trust);

        underTest.addTrust(stackId);

        verify(client).addTrust("secret", "ad", true, "REALM");
    }
}