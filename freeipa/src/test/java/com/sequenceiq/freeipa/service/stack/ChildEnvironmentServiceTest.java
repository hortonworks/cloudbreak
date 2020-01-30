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

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.registerchildenv.RegisterChildEnvironmentRequest;
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
    void isChildEnvironment_false() {
        when(repository.findParentByChildEnvironmentCrn(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.empty());

        boolean result = underTest.isChildEnvironment(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertFalse(result);
    }

    @Test
    void isChildEnvironment_true() {
        when(repository.findParentByChildEnvironmentCrn(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(Optional.of("id"));

        boolean result = underTest.isChildEnvironment(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertTrue(result);
    }

    @Test
    void registerChildEnvironment() {
        RegisterChildEnvironmentRequest request = new RegisterChildEnvironmentRequest();
        underTest.registerChildEnvironment(request, ACCOUNT_ID);

        verify(stackService).registerChildEnvironment(request, ACCOUNT_ID);
    }
}