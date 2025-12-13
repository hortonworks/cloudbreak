package com.sequenceiq.freeipa.service.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    private static final String OPERATION_ID = "op1";

    private static final String ACCOUNT_ID = "test_acc_id";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:envId1";

    private static final OperationType OPERATION_TYPE = OperationType.TRUST_SETUP;

    private static final PageRequest FIRST_RECORD = PageRequest.of(0, 1);

    @Mock
    private OperationRepository operationRepository;

    @Mock
    private List<OperationAcceptor> operationAcceptorList;

    @InjectMocks
    private OperationService underTest;

    private Operation operation = new Operation();

    @Test
    void getOperationForAccountIdAndOperationIdFound() {
        when(operationRepository.findByOperationIdAndAccountId(OPERATION_ID, ACCOUNT_ID))
                .thenReturn(Optional.of(operation));
        Operation result = underTest.getOperationForAccountIdAndOperationId(ACCOUNT_ID, OPERATION_ID);
        assertThat(result).isEqualTo(operation);
    }

    @Test
    void getOperationForAccountIdAndOperationIdNotFound() {
        when(operationRepository.findByOperationIdAndAccountId(OPERATION_ID, ACCOUNT_ID))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> underTest.getOperationForAccountIdAndOperationId(ACCOUNT_ID, OPERATION_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getLatestOperationForEnvironmentCrnAndOperationTypeFound() {
        when(operationRepository.findLatestByEnvironmentCrnAndOperationType(eq(ACCOUNT_ID), eq(ENV_CRN), eq(OPERATION_TYPE), eq(FIRST_RECORD)))
                .thenReturn(Optional.of(operation));
        Operation result = underTest.getLatestOperationForEnvironmentCrnAndOperationType(ENV_CRN, OPERATION_TYPE);
        assertThat(result).isEqualTo(operation);
    }

    @Test
    void getLatestOperationForEnvironmentCrnAndOperationTypeNotFound() {
        when(operationRepository.findLatestByEnvironmentCrnAndOperationType(eq(ACCOUNT_ID), eq(ENV_CRN), eq(OPERATION_TYPE), eq(FIRST_RECORD)))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> underTest.getLatestOperationForEnvironmentCrnAndOperationType(ENV_CRN, OPERATION_TYPE))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    public void testUpdateOperation() {
        ArrayList<SuccessDetails> origSuccessDetails = new ArrayList<>();
        origSuccessDetails.add(new SuccessDetails());
        ArrayList<FailureDetails> origFailureDetails = new ArrayList<>();
        origFailureDetails.add(new FailureDetails());
        operation.setFailureList(origFailureDetails);
        operation.setSuccessList(origSuccessDetails);
        when(operationRepository.findByOperationIdAndAccountId(OPERATION_ID, ACCOUNT_ID)).thenReturn(Optional.of(operation));
        when(operationRepository.save(operation)).thenReturn(operation);
        ArrayList<SuccessDetails> successDetails = new ArrayList<>();
        successDetails.add(new SuccessDetails());
        ArrayList<FailureDetails> failureDetails = new ArrayList<>();
        failureDetails.add(new FailureDetails());
        Operation actualOperation = underTest.updateOperation(ACCOUNT_ID, OPERATION_ID, successDetails, failureDetails);
        assertEquals(2, actualOperation.getSuccessList().size());
        assertEquals(2, actualOperation.getFailureList().size());
    }

    @Test
    public void testCompleteOperation() {
        operation.setStartTime(System.currentTimeMillis());
        when(operationRepository.findByOperationIdAndAccountId(OPERATION_ID, ACCOUNT_ID)).thenReturn(Optional.of(operation));
        when(operationRepository.save(operation)).thenReturn(operation);
        ArrayList<SuccessDetails> successDetails = new ArrayList<>();
        successDetails.add(new SuccessDetails());
        ArrayList<FailureDetails> failureDetails = new ArrayList<>();
        failureDetails.add(new FailureDetails());
        Operation actualOperation = underTest.completeOperation(ACCOUNT_ID, OPERATION_ID, successDetails, failureDetails);
        assertEquals(1, actualOperation.getSuccessList().size());
        assertEquals(1, actualOperation.getFailureList().size());
    }

    @Test
    public void testFailOperation() {
        ArrayList<SuccessDetails> origSuccessDetails = new ArrayList<>();
        origSuccessDetails.add(new SuccessDetails());
        origSuccessDetails.add(new SuccessDetails());
        ArrayList<FailureDetails> origFailureDetails = new ArrayList<>();
        origFailureDetails.add(new FailureDetails());
        origFailureDetails.add(new FailureDetails());
        operation.setFailureList(origFailureDetails);
        operation.setSuccessList(origSuccessDetails);
        operation.setStartTime(System.currentTimeMillis());
        when(operationRepository.findByOperationIdAndAccountId(OPERATION_ID, ACCOUNT_ID)).thenReturn(Optional.of(operation));
        when(operationRepository.save(operation)).thenReturn(operation);
        ArrayList<SuccessDetails> successDetails = new ArrayList<>();
        successDetails.add(new SuccessDetails());
        successDetails.add(new SuccessDetails());
        ArrayList<FailureDetails> failureDetails = new ArrayList<>();
        failureDetails.add(new FailureDetails());
        failureDetails.add(new FailureDetails());
        failureDetails.add(new FailureDetails());
        Operation actualOperation = underTest.failOperation(ACCOUNT_ID, OPERATION_ID, "failed", successDetails, failureDetails);
        assertEquals(4, actualOperation.getSuccessList().size());
        assertEquals(5, actualOperation.getFailureList().size());
    }
}
