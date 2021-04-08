package com.sequenceiq.freeipa.service.binduser;

import static com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserFlowEvent.CREATE_BIND_USER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.flow.freeipa.binduser.create.event.CreateBindUserEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class BindUserCreateServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String ENV_CRN = "envCrn";

    private static final String ACCOUNT = "accountId";

    @Mock
    private OperationService operationService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private OperationToOperationStatusConverter operationConverter;

    @Mock
    private StackService stackService;

    @InjectMocks
    private BindUserCreateService underTest;

    @BeforeEach
    public void init() {
        when(stackService.getIdByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT)).thenReturn(STACK_ID);
    }

    @Test
    public void testCreateStartedFLowNotified() {
        BindUserCreateRequest request = createRequest();
        Operation operation = new Operation();
        operation.setOperationId("op");
        operation.setStatus(OperationState.RUNNING);
        when(operationService.startOperation(eq(ACCOUNT), eq(OperationType.BIND_USER_CREATE), anyCollection(), anyCollection())).thenReturn(operation);
        OperationStatus operationStatus = new OperationStatus();
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus response = underTest.createBindUser(ACCOUNT, request);

        assertEquals(operationStatus, response);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq(CREATE_BIND_USER_EVENT.event()), captor.capture());
        Acceptable event = captor.getValue();
        assertTrue(event instanceof CreateBindUserEvent);
        CreateBindUserEvent bindUserEvent = (CreateBindUserEvent) event;
        assertEquals(CREATE_BIND_USER_EVENT.event(), bindUserEvent.selector());
        assertEquals(STACK_ID, bindUserEvent.getResourceId());
        assertEquals(ACCOUNT, bindUserEvent.getAccountId());
        assertEquals(operation.getOperationId(), bindUserEvent.getOperationId());
        assertEquals(request.getEnvironmentCrn(), bindUserEvent.getEnvironmentCrn());
        assertEquals(request.getBindUserNameSuffix(), bindUserEvent.getSuffix());
    }

    @Test
    public void testOperationRejected() {
        BindUserCreateRequest request = createRequest();
        Operation operation = new Operation();
        operation.setOperationId("op");
        operation.setStatus(OperationState.REJECTED);
        when(operationService.startOperation(eq(ACCOUNT), eq(OperationType.BIND_USER_CREATE), anyCollection(), anyCollection())).thenReturn(operation);
        OperationStatus operationStatus = new OperationStatus();
        when(operationConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus response = underTest.createBindUser(ACCOUNT, request);

        assertEquals(operationStatus, response);
        verifyNoInteractions(flowManager);
    }

    @Test
    public void testFlowRejected() {
        BindUserCreateRequest request = createRequest();
        Operation operation = new Operation();
        operation.setOperationId("op");
        operation.setStatus(OperationState.RUNNING);
        Operation failedOperation = new Operation();
        failedOperation.setOperationId("op");
        failedOperation.setStatus(OperationState.FAILED);
        when(operationService.startOperation(eq(ACCOUNT), eq(OperationType.BIND_USER_CREATE), anyCollection(), anyCollection())).thenReturn(operation);
        when(operationService.failOperation(ACCOUNT, operation.getOperationId(), "Couldn't start create bind user flow: Flow failure"))
                .thenReturn(failedOperation);
        OperationStatus failedOperationStatus = new OperationStatus();
        failedOperationStatus.setStatus(OperationState.FAILED);
        when(operationConverter.convert(failedOperation)).thenReturn(failedOperationStatus);
        when(flowManager.notify(anyString(), any(Acceptable.class))).thenThrow(new RuntimeException("Flow failure"));

        OperationStatus response = underTest.createBindUser(ACCOUNT, request);

        assertEquals(failedOperationStatus, response);
    }

    private BindUserCreateRequest createRequest() {
        BindUserCreateRequest request = new BindUserCreateRequest();
        request.setEnvironmentCrn(ENV_CRN);
        request.setBindUserNameSuffix("suffix");
        return request;
    }
}