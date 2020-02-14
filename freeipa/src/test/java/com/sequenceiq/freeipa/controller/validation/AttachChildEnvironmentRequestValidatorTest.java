package com.sequenceiq.freeipa.controller.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.ChildEnvironmentService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
class AttachChildEnvironmentRequestValidatorTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String CHILD_ENVIRONMENT_CRN = "crn:child";

    private static final String PARENT_ENVIRONMENT_CRN = "crn:parent";

    private static final AttachChildEnvironmentRequest REQUEST = new AttachChildEnvironmentRequest();

    static {
        REQUEST.setChildEnvironmentCrn(CHILD_ENVIRONMENT_CRN);
        REQUEST.setParentEnvironmentCrn(PARENT_ENVIRONMENT_CRN);
    }

    @InjectMocks
    private AttachChildEnvironmentRequestValidator underTest;

    @Mock
    private StackService stackService;

    @Mock
    private CrnService crnService;

    @Mock
    private ChildEnvironmentService childEnvironmentService;

    @BeforeEach
    void setUp() {
        lenient().when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void validateShouldContainErrors() {
        when(stackService.findAllByEnvironmentCrnAndAccountId(PARENT_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of());
        when(stackService.findAllByEnvironmentCrnAndAccountId(CHILD_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of(new Stack()));
        when(childEnvironmentService.isChildEnvironment(PARENT_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(true);
        when(childEnvironmentService.isChildEnvironment(CHILD_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(true);

        ValidationResult result = underTest.validate(REQUEST);

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrors())
                .contains(AttachChildEnvironmentRequestValidator.PARENT_ENVIRONMENT_SHOULD_HAVE_A_STACK)
                .contains(AttachChildEnvironmentRequestValidator.CHILD_ENVIRONMENT_ALREADY_HAS_A_STACK)
                .contains(AttachChildEnvironmentRequestValidator.PARENT_ENVIRONMENT_CAN_NOT_BE_A_CHILD_ENVIRONMENT)
                .contains(AttachChildEnvironmentRequestValidator.CHILD_ENVIRONMENT_ALREADY_HAS_A_PARENT_ENVIRONMENT);
    }

    @Test
    void validateShouldNotContainErrors() {
        when(stackService.findAllByEnvironmentCrnAndAccountId(PARENT_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of(new Stack()));
        when(stackService.findAllByEnvironmentCrnAndAccountId(CHILD_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(List.of());
        when(childEnvironmentService.isChildEnvironment(PARENT_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(false);
        when(childEnvironmentService.isChildEnvironment(CHILD_ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(false);

        ValidationResult result = underTest.validate(REQUEST);

        assertThat(result.hasError()).isFalse();
    }

}
