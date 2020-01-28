package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String ACCOUNT_ID = "account:id";

    @InjectMocks
    private StackService underTest;

    @Mock
    private StackRepository stackRepository;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
    }

    @Test
    void getByIdWithListsInTransactionNotFound() {
        when(stackRepository.findOneWithLists(STACK_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.getByIdWithListsInTransaction(STACK_ID));
        assertEquals("Stack [" + STACK_ID + "] not found", notFoundException.getMessage());
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
        assertEquals("Stack [" + STACK_ID + "] not found", notFoundException.getMessage());
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
    void getByAccountIdEnvironmentAndNameNotFound() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndName(ACCOUNT_ID, ENVIRONMENT_CRN, STACK_NAME)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.getByAccountIdEnvironmentAndName(ACCOUNT_ID, ENVIRONMENT_CRN, STACK_NAME));
        assertEquals("Stack [" + STACK_NAME + "] in environment [" + ENVIRONMENT_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void getByAccountIdEnvironmentAndName() {
        when(stackRepository.findByAccountIdEnvironmentCrnAndName(ACCOUNT_ID, ENVIRONMENT_CRN, STACK_NAME)).thenReturn(Optional.of(stack));
        Stack stackByAccountIdEnvironmentAndName = underTest.getByAccountIdEnvironmentAndName(ACCOUNT_ID, ENVIRONMENT_CRN, STACK_NAME);
        assertEquals(stack, stackByAccountIdEnvironmentAndName);
    }

    @Test
    void getByEnvironmentCrnNotFound() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException =
                assertThrows(NotFoundException.class, () -> underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertEquals("Stack by environment [" + ENVIRONMENT_CRN + "] not found", notFoundException.getMessage());
    }

    @Test
    void getByEnvironmentCrn() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));
        Stack stackByEnvironmentCrn = underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID);
        assertEquals(stack, stackByEnvironmentCrn);
    }

    @Test
    void getByEnvironmentCrnAndAccountIdWithLists() {
        // TODO
    }

    @Test
    void registerChildEnvironment() {
        // TODO
    }
}
