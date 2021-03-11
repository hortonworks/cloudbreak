package com.sequenceiq.freeipa.service.stack;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}