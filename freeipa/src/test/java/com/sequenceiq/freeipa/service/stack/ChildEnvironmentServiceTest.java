package com.sequenceiq.freeipa.service.stack;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.ChildEnvironmentRepository;

@ExtendWith(MockitoExtension.class)
class ChildEnvironmentServiceTest {

    private static final String ENVIRONMENT_CRN = "test:environment:crn";

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
        underTest.attachChildEnvironment(request, ACCOUNT_ID);

        verify(stackService).attachChildEnvironment(request, ACCOUNT_ID);
    }

    @Test
    void detachChildEnvironment() {
        DetachChildEnvironmentRequest request = new DetachChildEnvironmentRequest();
        underTest.detachChildEnvironment(request, ACCOUNT_ID);

        verify(stackService).detachChildEnvironment(request, ACCOUNT_ID);
    }
}