package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String CHILD_ENVIRONMENT_CRN = "test:environment:child-crn";

    private static final String FREEIPA_CRN = "test:freeipa:crn";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String ACCOUNT_ID = "account:id";

    private static final LocalDateTime MOCK_NOW = LocalDateTime.of(1969, 4, 1, 4, 20);

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private ChildEnvironmentService childEnvironmentService;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);

        underTest.nowSupplier = () -> MOCK_NOW;
    }

    @Test
    void getByIdWithListsInTransactionNotFound() {
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByIdWithListsInTransaction(STACK_ID));
        assertEquals("FreeIPA stack [" + STACK_ID + "] not found", notFoundException.getMessage());
    }

    @Test
    void getByIdWithListsInTransaction() {
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.of(stack));
        Stack stackByIdWithListsInTransaction = underTest.getByIdWithListsInTransaction(STACK_ID);
        assertEquals(stack, stackByIdWithListsInTransaction);
    }

    @Test
    void getStackByIdNotFound() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getStackById(STACK_ID));
        assertEquals("FreeIPA stack [" + STACK_ID + "] not found", notFoundException.getMessage());
    }

    @Test
    void getStackById() {
        when(stackRepository.findById(STACK_ID)).thenReturn(Optional.of(stack));
        Stack stackById = underTest.getStackById(STACK_ID);
        assertEquals(stack, stackById);
    }

    @Test
    void save() {
        when(stackRepository.save(stack)).thenReturn(stack);
        Stack savedStack = underTest.save(stack);
        assertEquals(stack, savedStack);
    }

    @Test
    void getByEnvironmentCrnNotFound() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(childEnvironmentService.findParentByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException =
                assertThrows(NotFoundException.class, () -> underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertEquals("FreeIPA stack by environment [" + ENVIRONMENT_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void getByEnvironmentCrnWhenOnlyParentsStackFound() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(childEnvironmentService.findParentByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));
        Stack stackByEnvironmentCrn = underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, stackByEnvironmentCrn);
    }

    @Test
    void getByEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));
        Stack stackByEnvironmentCrn = underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, stackByEnvironmentCrn);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithListsShouldReturnByEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        Stack result = underTest.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(stackRepository).findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(stackRepository, never()).findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, result);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithListsShouldReturnByChildEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(stackRepository.findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        Stack result = underTest.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(stackRepository).findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(stackRepository).findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, result);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithListsShouldFailAsFallback() {
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(stackRepository.findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID));
        verify(stackRepository).findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
        verify(stackRepository).findByChildEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID);
    }

    @Test
    void getImagesOfAliveStacksWithNoThresholdShouldCallRepositoryWithCurrentTimestamp() {
        underTest.getImagesOfAliveStacks(null);

        verify(stackRepository).findImagesOfAliveStacks(Timestamp.valueOf(MOCK_NOW).getTime());
    }

    @Test
    void getImagesOfAliveStacksWithThresholdShouldCallRepositoryWithModifiedTimestamp() {
        final int thresholdInDays = 180;
        final LocalDateTime thresholdTime = MOCK_NOW.minusDays(thresholdInDays);

        underTest.getImagesOfAliveStacks(thresholdInDays);

        verify(stackRepository).findImagesOfAliveStacks(Timestamp.valueOf(thresholdTime).getTime());
    }

    @Test
    void findAllWithDetailedStackStatuses() {
        when(stackRepository.findAllWithDetailedStackStatuses(Mockito.eq(List.of(DetailedStackStatus.AVAILABLE)))).thenReturn(List.of(stack));
        List<Stack> results = underTest.findAllWithDetailedStackStatuses(List.of(DetailedStackStatus.AVAILABLE));

        assertEquals(List.of(stack), results);
    }

    @Test
    void findByCrnAndAccountIdWithListsEvenIfTerminated() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.of(stack));
        Optional<Stack> results = underTest.findByCrnAndAccountIdWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN);

        assertEquals(Optional.of(stack), results);
    }

    @Test
    void getByCrnAndAccountIdEvenIfTerminated() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.of(stack));
        Stack results = underTest.getByCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN);

        assertEquals(stack, results);
    }

    @Test
    void getByCrnAndAccountIdEvenIfTerminatedThrowsWhenNotPresent() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.empty());
        when(childEnvironmentService.findParentStackByChildEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN))
                .thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.getByCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN));
        assertEquals("FreeIPA stack by environment [" + ENVIRONMENT_CRN + "] with CRN [" + FREEIPA_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList() {
        when(stackRepository.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of(stack));
        List<Stack> results = underTest.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminatedWithList(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(List.of(stack), results);
    }

    @Test
    void findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated() {
        when(stackRepository.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of(stack));
        List<Stack> results = underTest.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(List.of(stack), results);
    }
}
