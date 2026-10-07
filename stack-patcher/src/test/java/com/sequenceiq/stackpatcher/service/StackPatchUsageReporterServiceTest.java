package com.sequenceiq.stackpatcher.service;

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
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.stackpatcher.TestStackPatchResource;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;
import com.sequenceiq.stackpatcher.domain.StackPatchType;

@ExtendWith(MockitoExtension.class)
class StackPatchUsageReporterServiceTest {

    private static final String RESOURCE_CRN = "crn";

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.TEST_PATCH_1;

    private TestStackPatchResource stack;

    @Mock
    private UsageReporter usageReporter;

    @InjectMocks
    private StackPatchUsageReporterService underTest;

    @Captor
    private ArgumentCaptor<UsageProto.CDPStackPatchEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        stack = new TestStackPatchResource(1L, RESOURCE_CRN);
    }

    @Test
    void shouldReportAffected() {
        underTest.reportStatusChange(stack, STACK_PATCH_TYPE, StackPatchStatus.AFFECTED, "");

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.AFFECTED, "");
    }

    @Test
    void shouldReportSuccess() {
        underTest.reportStatusChange(stack, STACK_PATCH_TYPE, StackPatchStatus.FIXED, "");

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.SUCCESS, "");
    }

    @Test
    void shouldReportFailure() {
        String failureMessage = "failure message";

        underTest.reportStatusChange(stack, STACK_PATCH_TYPE, StackPatchStatus.FAILED, failureMessage);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.FAILURE, failureMessage);
    }

    @Test
    void affectedShouldNotFailWithNullValues() {
        underTest.reportStatusChange(null, STACK_PATCH_TYPE, StackPatchStatus.AFFECTED, null);
    }

    @Test
    void customAffectedShouldNotFailWithNullValues() {
        underTest.reportUsage(null, null, null, null);
    }

    @Test
    void shouldReportCustomFailure() {
        String failureMessage = "failure message";
        underTest.reportUsage(stack, StackPatchType.TEST_PATCH_1, UsageProto.CDPStackPatchEventType.Value.FAILURE, failureMessage);

        verify(usageReporter).cdpStackPatcherEvent(eventCaptor.capture());
        Assertions.assertThat(eventCaptor.getValue())
                .returns(stack.getResourceCrn(), UsageProto.CDPStackPatchEvent::getResourceCrn)
                .returns(StackPatchType.TEST_PATCH_1.name(), UsageProto.CDPStackPatchEvent::getStackPatchType)
                .returns(UsageProto.CDPStackPatchEventType.Value.FAILURE, UsageProto.CDPStackPatchEvent::getEventType)
                .returns(failureMessage, UsageProto.CDPStackPatchEvent::getMessage);
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
