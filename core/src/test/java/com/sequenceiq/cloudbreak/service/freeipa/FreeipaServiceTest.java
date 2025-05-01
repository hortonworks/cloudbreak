package com.sequenceiq.cloudbreak.service.freeipa;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.FREEIPA_LDAP_BIND_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;

@ExtendWith(MockitoExtension.class)
class FreeipaServiceTest {

    private static final String ENV_CRN =
            "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String FREEIPA_CRN = "freeIpaCRN";

    private static final String FREEIPA_FLOW_ID = "flowId";

    private static final String ROTATE_ERROR_REASON = "rotate error reason";

    private static final String CURRENT_STATE = "currentState";

    @Mock
    private FreeipaClientService freeipaClientService;

    @InjectMocks
    private FreeipaService underTest;

    @ParameterizedTest
    @EnumSource(value = Status.class)
    void testCheckFreeipaRunningWhenFreeIpaStoppedThenReturnsFalse(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        boolean freeipaRunning = underTest.checkFreeipaRunning(ENV_CRN);
        assertFalse(freeipaRunning);
    }

    @ParameterizedTest
    @EnumSource(value = Status.class)
    void testCheckFreeipaRunningWhenFreeIpaUnknownThenThrowsException(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.UNKNOWN);

        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaIsNullThenThrowsException() {
        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(null);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @Test
    void testCheckFreeipaRunningWhenFreeIpaStatusIsNullThenThrowsException() {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class, () -> underTest.checkFreeipaRunning(ENV_CRN));
        assertEquals("Freeipa availability cannot be determined currently.", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class)
    void testCheckFreeipaRunningWhenFreeIpaAvailableThenPass(Status status) {
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setStatus(status);
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeipa);

        Assertions.assertDoesNotThrow(() -> underTest.checkFreeipaRunning(ENV_CRN));
    }

    @Test
    void rotateFreeIpaSecretsShouldFailIfRedbeamsFlowChainIsNotTriggered() {
        when(freeipaClientService.rotateSecret(any(), any())).thenReturn(new FlowIdentifier(FlowType.NOT_TRIGGERED, null));
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateFreeIpaSecret(ENV_CRN, FREEIPA_LDAP_BIND_PASSWORD, ROTATE, Map.of()));
        String expected = String.format("FreeIpa flow failed with error: 'Flow null not triggered'. "
                        + "Environment crn: %s, flow: FlowIdentifier{type=%s, pollableId='%s'}",
                ENV_CRN, FlowType.NOT_TRIGGERED, null);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateFreeIpaSecretsShouldFailIfReturnedFlowInformationIsNull() {
        when(freeipaClientService.rotateSecret(any(), any())).thenReturn(null);
        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateFreeIpaSecret(ENV_CRN, FREEIPA_LDAP_BIND_PASSWORD, ROTATE, Map.of()));
        String expected = String.format("FreeIpa flow failed with error: 'unknown'. Environment crn: %s, flow: null", ENV_CRN);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateFreeIpaSecretsShouldFailIfRedbeamsFlowChainFailed() {
        when(freeipaClientService.rotateSecret(any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, FREEIPA_FLOW_ID));
        when(freeipaClientService.hasFlowChainRunningByFlowChainId(FREEIPA_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.TRUE));
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        freeIpaResponse.setCrn(FREEIPA_CRN);
        freeIpaResponse.setStatusReason(ROTATE_ERROR_REASON);
        when(freeipaClientService.getByEnvironmentCrn(ENV_CRN)).thenReturn(freeIpaResponse);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class,
                () -> underTest.rotateFreeIpaSecret(ENV_CRN, FREEIPA_LDAP_BIND_PASSWORD, ROTATE, Map.of()));

        String expected = String.format("FreeIpa flow failed with error: '%s'. Environment crn: %s, "
                        + "flow: FlowIdentifier{type=%s, pollableId='%s'}",
                ROTATE_ERROR_REASON, ENV_CRN, FlowType.FLOW_CHAIN, FREEIPA_FLOW_ID);
        assertEquals(expected, cloudbreakServiceException.getMessage());
    }

    @Test
    void rotateFreeIpaSecretsShouldSucceed() {
        when(freeipaClientService.rotateSecret(any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW_CHAIN, FREEIPA_FLOW_ID));
        when(freeipaClientService.hasFlowChainRunningByFlowChainId(FREEIPA_FLOW_ID)).thenReturn(createFlowCheckResponse(Boolean.FALSE, Boolean.FALSE));

        underTest.rotateFreeIpaSecret(ENV_CRN, FREEIPA_LDAP_BIND_PASSWORD, ROTATE, Map.of());

        ArgumentCaptor<String> envCrnCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<FreeIpaSecretRotationRequest> requestCaptor = ArgumentCaptor.forClass(FreeIpaSecretRotationRequest.class);
        verify(freeipaClientService, times(1)).rotateSecret(envCrnCaptor.capture(), requestCaptor.capture());
        FreeIpaSecretRotationRequest request = requestCaptor.getValue();
        assertEquals(ENV_CRN, envCrnCaptor.getValue());
        assertEquals(FREEIPA_LDAP_BIND_PASSWORD.name(), request.getSecrets().get(0));
        assertEquals(ROTATE, request.getExecutionType());
    }

    @Test
    void preValidateShouldFailIfEnvironmentCrnIsNull() {
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class, () -> underTest.preValidateFreeIpaSecretRotation(null));
        assertEquals("No environment crn found, rotation is not possible.", secretRotationException.getMessage());
        verify(freeipaClientService, never()).getLastFlowId(eq(ENV_CRN));
    }

    @Test
    void preValidateShouldFailIfLastFlowIsRunningInFreeIpa() {
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.PENDING);
        lastFlow.setCurrentState(CURRENT_STATE);
        when(freeipaClientService.getLastFlowId(eq(ENV_CRN))).thenReturn(lastFlow);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidateFreeIpaSecretRotation(ENV_CRN));
        assertEquals(String.format("Polling in FreeIpa is not possible since last known state of flow for the FreeIpa is %s", CURRENT_STATE),
                secretRotationException.getMessage());
        verify(freeipaClientService, times(1)).getLastFlowId(eq(ENV_CRN));
    }

    @Test
    void preValidateShouldSucceedIfLastFlowIsNotRunningInRedbeams() {
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.SUCCESSFUL);
        when(freeipaClientService.getLastFlowId(eq(ENV_CRN))).thenReturn(lastFlow);
        underTest.preValidateFreeIpaSecretRotation(ENV_CRN);
        verify(freeipaClientService, times(1)).getLastFlowId(eq(ENV_CRN));
    }

    @Test
    void preValidateShouldSucceedIfLastFlowIsMissingInRedbeams() {
        when(freeipaClientService.getLastFlowId(eq(ENV_CRN))).thenReturn(null);
        underTest.preValidateFreeIpaSecretRotation(ENV_CRN);
        verify(freeipaClientService, times(1)).getLastFlowId(eq(ENV_CRN));
    }

    private FlowCheckResponse createFlowCheckResponse(Boolean hasActiveFlow, Boolean failed) {
        FlowCheckResponse flowResp = new FlowCheckResponse();
        flowResp.setFlowId(FREEIPA_FLOW_ID);
        flowResp.setHasActiveFlow(hasActiveFlow);
        flowResp.setLatestFlowFinalizedAndFailed(failed);
        return flowResp;
    }
}