package com.sequenceiq.freeipa.service.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
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

    @Mock
    private Operation operation;

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

}
