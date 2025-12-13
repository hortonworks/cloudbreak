package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class StackPatchUsageReporterServiceTest {

    private static final String RESOURCE_CRN = "crn";

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.UNBOUND_RESTART;

    private Stack stack;

    @Mock
    private UsageReporter usageReporter;

    @InjectMocks
    private StackPatchUsageReporterService underTest;

    @Captor
    private ArgumentCaptor<UsageProto.CDPStackPatchEvent> eventCaptor;

    private StackPatch stackPatch;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setResourceCrn(RESOURCE_CRN);
        stackPatch = new StackPatch(stack, STACK_PATCH_TYPE);
    }

    @Test
    void shouldReportAffected() {
        stackPatch.setStatus(StackPatchStatus.AFFECTED);

        underTest.reportUsage(stackPatch);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.AFFECTED, "");
    }

    @Test
    void shouldReportSuccess() {
        stackPatch.setStatus(StackPatchStatus.FIXED);

        underTest.reportUsage(stackPatch);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.SUCCESS, "");
    }

    @Test
    void shouldReportFailure() {
        String failureMessage = "failure message";
        stackPatch.setStatus(StackPatchStatus.FAILED);
        stackPatch.setStatusReason(failureMessage);

        underTest.reportUsage(stackPatch);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.FAILURE, failureMessage);
    }

    @Test
    void affectedShouldNotFailWithNullValues() {
        stackPatch.setStack(null);

        underTest.reportUsage(stackPatch);
    }

    private void verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value eventType, String message) {
        verify(usageReporter).cdpStackPatcherEvent(eventCaptor.capture());
        Assertions.assertThat(eventCaptor.getValue())
                .returns(RESOURCE_CRN, UsageProto.CDPStackPatchEvent::getResourceCrn)
                .returns(STACK_PATCH_TYPE.name(), UsageProto.CDPStackPatchEvent::getStackPatchType)
                .returns(eventType, UsageProto.CDPStackPatchEvent::getEventType)
                .returns(message, UsageProto.CDPStackPatchEvent::getMessage);
    }

}
