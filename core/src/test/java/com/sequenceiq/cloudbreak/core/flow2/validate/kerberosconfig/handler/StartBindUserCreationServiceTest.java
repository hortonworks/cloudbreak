package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.BIND_USER_CREATION_STARTED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.PollBindUserCreationEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.retry.RetryException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

@ExtendWith(MockitoExtension.class)
class StartBindUserCreationServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:accountId:environment:4c5ba74b-c35e-45e9-9f47-123456789876";

    private static final String STACK_NAME = "clusterName";

    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "opId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private StartBindUserCreationService underTest;

    private StackView stackView = mock(StackView.class);

    @BeforeEach
    public void init() {
        when(stackView.getResourceCrn()).thenReturn(ENV_CRN);
        when(stackView.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stackView.getName()).thenReturn(STACK_NAME);
        when(stackView.getId()).thenReturn(STACK_ID);
    }

    @AfterEach
    public void verifyAll() {
        ArgumentCaptor<BindUserCreateRequest> argumentCaptor = ArgumentCaptor.forClass(BindUserCreateRequest.class);
        verify(freeIpaV1Endpoint).createBindUser(argumentCaptor.capture(), eq(USER_CRN));
        BindUserCreateRequest request = argumentCaptor.getValue();
        assertEquals(ENV_CRN, request.getEnvironmentCrn());
        assertEquals(STACK_NAME, request.getBindUserNameSuffix());
    }

    @Test
    public void testOperationRunning() {
        OperationStatus operationStatus = new OperationStatus(OPERATION_ID, OperationType.BIND_USER_CREATE, OperationState.RUNNING, List.of(), List.of(),
                null, 1L, null);
        when(freeIpaV1Endpoint.createBindUser(any(BindUserCreateRequest.class), anyString())).thenReturn(operationStatus);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackEvent result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startBindUserCreation(stackView));

        assertEquals(BIND_USER_CREATION_STARTED_EVENT.event(), result.selector());
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, ((PollBindUserCreationEvent) result).getOperationId());
    }

    @Test
    public void testOperationFailed() {
        OperationStatus operationStatus = new OperationStatus(OPERATION_ID, OperationType.BIND_USER_CREATE, OperationState.FAILED, List.of(), List.of(),
                "errMsg", 1L, 2L);
        when(freeIpaV1Endpoint.createBindUser(any(BindUserCreateRequest.class), anyString())).thenReturn(operationStatus);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackEvent result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startBindUserCreation(stackView));

        assertEquals(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), result.selector());
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals("Failed to start bind user creation operation: errMsg", ((StackFailureEvent) result).getException().getMessage());
    }

    @Test
    public void testOperationRejected() {
        OperationStatus operationStatus = new OperationStatus(OPERATION_ID, OperationType.BIND_USER_CREATE, OperationState.REJECTED, List.of(), List.of(),
                "errMsg", 1L, 2L);
        when(freeIpaV1Endpoint.createBindUser(any(BindUserCreateRequest.class), anyString())).thenReturn(operationStatus);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        assertThrows(RetryException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.startBindUserCreation(stackView)));
    }
}