package com.sequenceiq.freeipa.flow.freeipa.binduser.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.user.AcceptResult;

@ExtendWith(MockitoExtension.class)
@Disabled
class BindUserCreateOperationAcceptorTest {

    private static final String ENV_CRN = "ENV_CRN";

    private static final String SUFFIX = "cluster";

    private static final String ACCOUNT = "accountId";

    @Mock
    private OperationRepository repository;

    @InjectMocks
    private BindUserCreateOperationAcceptor underTest;

    @Test
    public void testRejectedAlreadyRunning() {
        Operation runningOperation = createCurrentOperation();
        runningOperation.setId(0L);
        runningOperation.setOperationId("other");
        Operation currentOperation = createCurrentOperation();
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(runningOperation, currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertFalse(result.isAccepted());
        assertEquals("There is already a running bind user creation for cluster", result.getRejectionMessage().get());
    }

    @Test
    public void testMissingEnv() {
        Operation currentOperation = createCurrentOperation();
        currentOperation.setEnvironmentList(null);
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertFalse(result.isAccepted());
        assertEquals("Bind user create must run only for one environment!", result.getRejectionMessage().get());
    }

    @Test
    public void testMultipleEnv() {
        Operation currentOperation = createCurrentOperation();
        currentOperation.setEnvironmentList(List.of(ENV_CRN, "env2"));
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertFalse(result.isAccepted());
        assertEquals("Bind user create must run only for one environment!", result.getRejectionMessage().get());
    }

    @Test
    public void testMissingSuffix() {
        Operation currentOperation = createCurrentOperation();
        currentOperation.setUserList(null);
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertFalse(result.isAccepted());
        assertEquals("Bind user create must run only for one suffix!", result.getRejectionMessage().get());
    }

    @Test
    public void testMultipleSuffix() {
        Operation currentOperation = createCurrentOperation();
        currentOperation.setUserList(List.of(SUFFIX, "cluster2"));
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertFalse(result.isAccepted());
        assertEquals("Bind user create must run only for one suffix!", result.getRejectionMessage().get());
    }

    @Test
    public void testDifferentEnvSameSuffixIsAccepted() {
        Operation runningOperation = createCurrentOperation();
        runningOperation.setId(0L);
        runningOperation.setOperationId("other");
        runningOperation.setEnvironmentList(List.of("otherEnv"));
        Operation currentOperation = createCurrentOperation();
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(runningOperation, currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertTrue(result.isAccepted());
        assertTrue(result.getRejectionMessage().isEmpty());
    }

    @Test
    public void testSameEnvDifferentSuffixIsAccepted() {
        Operation runningOperation = createCurrentOperation();
        runningOperation.setId(0L);
        runningOperation.setOperationId("other");
        runningOperation.setUserList(List.of("cluster2"));
        Operation currentOperation = createCurrentOperation();
        when(repository.findRunningByAccountIdAndType(ACCOUNT, underTest.selector())).thenReturn(List.of(runningOperation, currentOperation));

        AcceptResult result = underTest.accept(currentOperation);

        assertTrue(result.isAccepted());
        assertTrue(result.getRejectionMessage().isEmpty());
    }

    private Operation createCurrentOperation() {
        Operation operation = new Operation();
        operation.setId(1L);
        operation.setOperationId("id1");
        operation.setAccountId(ACCOUNT);
        operation.setOperationType(OperationType.BIND_USER_CREATE);
        operation.setEnvironmentList(List.of(ENV_CRN));
        operation.setUserList(List.of(SUFFIX));
        return operation;
    }
}