package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.IntNode;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.common.type.KdcType;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.ActiveDirectoryTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.BaseClusterTrustSetupCommands;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.Trust;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.crossrealm.TrustCommandType;
import com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory.ActiveDirectoryBaseClusterTrustCommandsBuilder;
import com.sequenceiq.freeipa.service.crossrealm.commands.activedirectory.ActiveDirectoryTrustInstructionsBuilder;
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

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private ActiveDirectoryTrustInstructionsBuilder activeDirectoryTrustInstructionsBuilder;

    @Mock
    private ActiveDirectoryBaseClusterTrustCommandsBuilder activeDirectoryBaseClusterTrustCommandsBuilder;

    @Test
    void returnsCommandsResponseWithADExpectedFields() {
        Stack stack = mock(Stack.class);
        FreeIpa freeIpa = mock(FreeIpa.class);
        LoadBalancer loadBalancer = mock(LoadBalancer.class);
        CrossRealmTrust crossRealmTrust = mock(CrossRealmTrust.class);
        ActiveDirectoryTrustSetupCommands activeDirectoryTrustSetupCommands = new ActiveDirectoryTrustSetupCommands();
        BaseClusterTrustSetupCommands baseClusterTrustSetupCommands = new BaseClusterTrustSetupCommands();
        when(activeDirectoryTrustInstructionsBuilder.buildInstructions(TrustCommandType.SETUP, stack, freeIpa, crossRealmTrust))
                .thenReturn(activeDirectoryTrustSetupCommands);
        when(activeDirectoryBaseClusterTrustCommandsBuilder.buildBaseClusterCommands(stack, TrustCommandType.SETUP, freeIpa, crossRealmTrust, loadBalancer))
                .thenReturn(baseClusterTrustSetupCommands);

        TrustSetupCommandsResponse response = underTest.buildTrustSetupCommandsResponse(TrustCommandType.SETUP, "env-crn", stack, freeIpa,
                crossRealmTrust, loadBalancer);

        assertEquals("env-crn", response.getEnvironmentCrn());
        assertEquals(KdcType.ACTIVE_DIRECTORY.name(), response.getKdcType());
        assertEquals(activeDirectoryTrustSetupCommands, response.getActiveDirectoryCommands());
        assertEquals(baseClusterTrustSetupCommands, response.getBaseClusterCommands());
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
        assertEquals(KdcType.ACTIVE_DIRECTORY, type);
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
        assertEquals("trustsetup.adtrust_install", capturedParams.getState());
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

    @Test
    void testDeleteTrust() throws Exception {
        Long stackId = 1L;
        String realm = "ad.realm";
        String realmUpper = realm.toUpperCase(Locale.ROOT);

        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stackId)).thenReturn(crossRealmTrust);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(crossRealmTrust.getKdcRealm()).thenReturn(realm);

        underTest.deleteTrust(stackId);

        verify(stackService).getByIdWithListsInTransaction(stackId);
        verify(crossRealmTrustService).getByStackId(stackId);
        verify(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        verify(freeIpaClient).deleteTrust(realmUpper);
    }

    @Test
    void testDeleteTrustIgnoreNotFound() throws Exception {
        Long stackId = 1L;
        String realm = "ad.realm";
        String realmUpper = realm.toUpperCase(Locale.ROOT);

        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stackId)).thenReturn(crossRealmTrust);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(crossRealmTrust.getKdcRealm()).thenReturn(realm);

        when(freeIpaClient.deleteTrust(realmUpper)).thenThrow(buildIpaException(FreeIpaErrorCodes.NOT_FOUND, "Not found"));

        underTest.deleteTrust(stackId);

        verify(stackService).getByIdWithListsInTransaction(stackId);
        verify(crossRealmTrustService).getByStackId(stackId);
        verify(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        verify(freeIpaClient).deleteTrust(realmUpper);
    }

    private FreeIpaClientException buildIpaException(FreeIpaErrorCodes freeIpaErrorCodes, String message) {
        JsonRpcClientException cause =
                new JsonRpcClientException(freeIpaErrorCodes.getValue(), "", new IntNode(freeIpaErrorCodes.getValue()));
        return new FreeIpaClientException(message, cause, OptionalInt.of(freeIpaErrorCodes.getValue()));
    }

    @Test
    void testDeleteTrustThrowForError() throws Exception {
        Long stackId = 1L;
        String realm = "ad.realm";
        String realmUpper = realm.toUpperCase(Locale.ROOT);

        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stackId)).thenReturn(crossRealmTrust);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(crossRealmTrust.getKdcRealm()).thenReturn(realm);

        when(freeIpaClient.deleteTrust(realmUpper))
                .thenThrow(buildIpaException(FreeIpaErrorCodes.NETWORK_ERROR, "Network error"));

        assertThrows(FreeIpaClientException.class, () -> underTest.deleteTrust(stackId));

        verify(stackService).getByIdWithListsInTransaction(stackId);
        verify(crossRealmTrustService).getByStackId(stackId);
        verify(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        verify(freeIpaClient).deleteTrust(realmUpper);
    }

    @Test
    void testDeleteDnsZonesSuccess() throws Exception {
        Long stackId = 1L;
        String realm = "ad.realm";
        String realmUpper = realm.toUpperCase(Locale.ROOT);

        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stackId)).thenReturn(crossRealmTrust);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(crossRealmTrust.getKdcRealm()).thenReturn(realm);

        underTest.deleteDnsZones(stackId);

        verify(stackService).getByIdWithListsInTransaction(stackId);
        verify(crossRealmTrustService).getByStackId(stackId);
        verify(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        verify(freeIpaClient).deleteForwardDnsZone("in-addr.arpa.");
        verify(freeIpaClient).deleteForwardDnsZone(realmUpper);
    }

    @Test
    void testDeleteDnsZonesIgnoreNotFound() throws Exception {
        Long stackId = 2L;
        String realm = "ad.realm";
        String realmUpper = realm.toUpperCase(Locale.ROOT);

        when(stackService.getByIdWithListsInTransaction(stackId)).thenReturn(stack);
        when(crossRealmTrustService.getByStackId(stackId)).thenReturn(crossRealmTrust);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(crossRealmTrust.getKdcRealm()).thenReturn(realm);

        doThrow(buildIpaException(FreeIpaErrorCodes.NOT_FOUND, "Not found"))
                .when(freeIpaClient).deleteForwardDnsZone("in-addr.arpa.");
        doThrow(buildIpaException(FreeIpaErrorCodes.NOT_FOUND, "Not found"))
                .when(freeIpaClient).deleteForwardDnsZone(realmUpper);

        underTest.deleteDnsZones(stackId);

        verify(stackService).getByIdWithListsInTransaction(stackId);
        verify(crossRealmTrustService).getByStackId(stackId);
        verify(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        verify(freeIpaClient).deleteForwardDnsZone("in-addr.arpa.");
        verify(freeIpaClient).deleteForwardDnsZone(realmUpper);
    }
}