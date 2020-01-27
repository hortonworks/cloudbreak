package com.sequenceiq.freeipa.service.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.repository.OperationRepository;

@ExtendWith(MockitoExtension.class)
class OperationStatusServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String INTERNAL_USER_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Mock
    OperationRepository operationRepository;

    @InjectMocks
    OperationStatusService underTest;

    @Test
    void getOperationFound() {
        String operationId = UUID.randomUUID().toString();
        Operation operation = mock(Operation.class);
        when(operationRepository.findByOperationIdAndAccountId(operationId, ACCOUNT_ID)).thenReturn(Optional.of(operation));

        assertEquals(operation, underTest.getOperationForAccountIdAndOperationId(USER_CRN, ACCOUNT_ID, operationId));
    }

    @Test
    void getOperationNotFound() {
        String operationId = UUID.randomUUID().toString();
        when(operationRepository.findByOperationIdAndAccountId(operationId, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getOperationForAccountIdAndOperationId(USER_CRN, ACCOUNT_ID, operationId));
    }

    @Test
    void getOperationInternalUser() {
        String operationId = UUID.randomUUID().toString();
        Operation operation = mock(Operation.class);
        when(operationRepository.findByOperationIdAndAccountId(operationId, ACCOUNT_ID)).thenReturn(Optional.of(operation));

        assertEquals(operation, underTest.getOperationForAccountIdAndOperationId(INTERNAL_USER_CRN, ACCOUNT_ID, operationId));
    }

    @Test
    void getOperationWrongAccount() {
        String operationId = UUID.randomUUID().toString();
        String wrongAccountId = UUID.randomUUID().toString();

        assertThrows(NotFoundException.class, () -> underTest.getOperationForAccountIdAndOperationId(USER_CRN, wrongAccountId, operationId));
    }

}