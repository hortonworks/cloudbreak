package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class StackServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String CHILD_ENVIRONMENT_CRN = "test:environment:child-crn";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "stack-name";

    private static final String ACCOUNT_ID = "account:id";

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
    void getByEnvironmentCrnNotFound() {
        when(stackRepository.findByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(childEnvironmentService.findParentByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());
        NotFoundException notFoundException =
                assertThrows(NotFoundException.class, () -> underTest.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID));
        assertEquals("Stack by environment [" + ENVIRONMENT_CRN + "] not found", notFoundException.getMessage());
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
    void attachChildEnvironmentShouldSucceed() {
        AttachChildEnvironmentRequest attachChildEnvironmentRequest = createAttachChildEnvironmentRequest();
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        underTest.attachChildEnvironment(attachChildEnvironmentRequest, ACCOUNT_ID);

        Assertions.assertThat(stack.getChildEnvironmentCrns()).contains(CHILD_ENVIRONMENT_CRN);
        verify(stackRepository).save(stack);
    }

    @Test
    void attachChildEnvironmentShouldFail() {
        AttachChildEnvironmentRequest attachChildEnvironmentRequest = createAttachChildEnvironmentRequest();
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.attachChildEnvironment(attachChildEnvironmentRequest, ACCOUNT_ID));
    }

    @Test
    void unattachChildEnvironmentShouldSucceed() {
        stack.attachChildEnvironment(CHILD_ENVIRONMENT_CRN);

        DetachChildEnvironmentRequest detachChildEnvironmentRequest = createDetachChildEnvironmentRequest();
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        underTest.detachChildEnvironment(detachChildEnvironmentRequest, ACCOUNT_ID);

        Assertions.assertThat(stack.getChildEnvironmentCrns()).doesNotContain(CHILD_ENVIRONMENT_CRN);
        verify(stackRepository).save(stack);
    }

    @Test
    void detachChildEnvironmentShouldFailOnMissingParent() {
        DetachChildEnvironmentRequest detachChildEnvironmentRequest = createDetachChildEnvironmentRequest();
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.detachChildEnvironment(detachChildEnvironmentRequest, ACCOUNT_ID));
    }

    @Test
    void detachChildEnvironmentShouldFailOnMissingChild() {
        DetachChildEnvironmentRequest detachChildEnvironmentRequest = createDetachChildEnvironmentRequest();
        when(stackRepository.findByEnvironmentCrnAndAccountIdWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(stack));

        assertThrows(NotFoundException.class, () -> underTest.detachChildEnvironment(detachChildEnvironmentRequest, ACCOUNT_ID));
    }

    private AttachChildEnvironmentRequest createAttachChildEnvironmentRequest() {
        AttachChildEnvironmentRequest attachChildEnvironmentRequest = new AttachChildEnvironmentRequest();
        attachChildEnvironmentRequest.setParentEnvironmentCrn(ENVIRONMENT_CRN);
        attachChildEnvironmentRequest.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);
        return attachChildEnvironmentRequest;
    }

    private DetachChildEnvironmentRequest createDetachChildEnvironmentRequest() {
        DetachChildEnvironmentRequest detachChildEnvironmentRequest = new DetachChildEnvironmentRequest();
        detachChildEnvironmentRequest.setParentEnvironmentCrn(ENVIRONMENT_CRN);
        detachChildEnvironmentRequest.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);
        return detachChildEnvironmentRequest;
    }
}
