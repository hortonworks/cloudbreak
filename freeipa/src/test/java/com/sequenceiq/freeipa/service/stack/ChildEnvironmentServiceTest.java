package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ChildEnvironmentRepository;

@ExtendWith(MockitoExtension.class)
class ChildEnvironmentServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

    private static final String CHILD_ENVIRONMENT_CRN = "test:childenv:crn";

    private static final String FREEIPA_CRN = "test:freeipa:crn";

    private static final String ACCOUNT_ID = "account:id";

    @InjectMocks
    private ChildEnvironmentService underTest;

    @Mock
    private ChildEnvironmentRepository repository;

    @Mock
    private StackService stackService;

    @Test
    void isChildEnvironmentFalse() {
        when(repository.findParentStackByChildEnvironmentCrn(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        boolean result = underTest.isChildEnvironment(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertFalse(result);
    }

    @Test
    void isChildEnvironmentTrue() {
        when(repository.findParentStackByChildEnvironmentCrn(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(new Stack()));

        boolean result = underTest.isChildEnvironment(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertTrue(result);
    }

    @Test
    void findMultipleParentStackByChildEnvironmentCrnWithListsEvenIfTerminated() {
        List<Stack> stackList = List.of(new Stack());
        when(repository.findMultipleParentByEnvironmentCrnEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stackList);

        List<Stack> result = underTest.findMultipleParentStackByChildEnvironmentCrnEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(stackList, result);
    }

    @Test
    void findMultipleParentStackByChildEnvironmentCrnWithListsEvenIfTerminatedWithLIst() {
        List<Stack> stackList = List.of(new Stack());
        when(repository.findMultipleParentByEnvironmentCrnEvenIfTerminatedWithList(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stackList);

        List<Stack> result = underTest.findMultipleParentStackByChildEnvironmentCrnEvenIfTerminatedWithList(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertEquals(stackList, result);
    }

    @Test
    void findParentStackByChildEnvironmentCrnAndCrnWithListsEvenIfTerminated() {
        Optional<Stack> stack = Optional.of(new Stack());
        when(repository.findParentByEnvironmentCrnAndCrnWthListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN)).thenReturn(stack);

        Optional<Stack> result = underTest.findParentStackByChildEnvironmentCrnAndCrnWithListsEvenIfTerminated(ENVIRONMENT_CRN, ACCOUNT_ID, FREEIPA_CRN);

        assertEquals(stack, result);
    }

    @Test
    void attachChildEnvironment() {
        AttachChildEnvironmentRequest request = new AttachChildEnvironmentRequest();
        request.setParentEnvironmentCrn(ENVIRONMENT_CRN);
        request.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);

        Stack stack = new Stack();
        when(stackService.getByOwnEnvironmentCrnAndAccountIdWithLists(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        underTest.attachChildEnvironment(request, ACCOUNT_ID);

        ArgumentCaptor<ChildEnvironment> childEnvironmentArgumentCaptor = ArgumentCaptor.forClass(ChildEnvironment.class);
        verify(repository).save(childEnvironmentArgumentCaptor.capture());
        ChildEnvironment childEnvironment = childEnvironmentArgumentCaptor.getValue();
        Assertions.assertThat(childEnvironment)
                .returns(CHILD_ENVIRONMENT_CRN, ChildEnvironment::getEnvironmentCrn)
                .returns(stack, ChildEnvironment::getStack);
    }

    @Test
    void detachChildEnvironmentSuccess() {
        ChildEnvironment childEnvironment = new ChildEnvironment();
        when(repository.findByParentAndChildEnvironmentCrns(ENVIRONMENT_CRN, CHILD_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of(childEnvironment));

        DetachChildEnvironmentRequest request = new DetachChildEnvironmentRequest();
        request.setParentEnvironmentCrn(ENVIRONMENT_CRN);
        request.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);

        underTest.detachChildEnvironment(request, ACCOUNT_ID);

        verify(repository).delete(childEnvironment);
    }

    @Test
    void detachChildEnvironmentFailure() {
        when(repository.findByParentAndChildEnvironmentCrns(ENVIRONMENT_CRN, CHILD_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        DetachChildEnvironmentRequest request = new DetachChildEnvironmentRequest();
        request.setParentEnvironmentCrn(ENVIRONMENT_CRN);
        request.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);

        Assertions.assertThatThrownBy(() -> underTest.detachChildEnvironment(request, ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void detachChildEnvironmentOptimisticLockingFailure() {
        ChildEnvironment childEnvironment = new ChildEnvironment();
        childEnvironment.setEnvironmentCrn(CHILD_ENVIRONMENT_CRN);
        when(repository.findByParentAndChildEnvironmentCrns(ENVIRONMENT_CRN, CHILD_ENVIRONMENT_CRN, ACCOUNT_ID))
                .thenReturn(Optional.of(childEnvironment));
        doThrow(ObjectOptimisticLockingFailureException.class).when(repository).delete(childEnvironment);

        DetachChildEnvironmentRequest request = new DetachChildEnvironmentRequest();
        request.setParentEnvironmentCrn(ENVIRONMENT_CRN);
        request.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);

        Assertions.assertThatThrownBy(() -> underTest.detachChildEnvironment(request, ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Child env %s is already detached", CHILD_ENVIRONMENT_CRN);
    }
}