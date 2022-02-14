package com.sequenceiq.cloudbreak.service.stackpatch;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
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

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setResourceCrn(RESOURCE_CRN);
    }

    @Test
    void shouldReportAffected() {
        underTest.reportAffected(stack, STACK_PATCH_TYPE);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.AFFECTED, "");
    }

    @Test
    void shouldReportSuccess() {
        underTest.reportSuccess(stack, STACK_PATCH_TYPE);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.SUCCESS, "");
    }

    @Test
    void shouldReportFailure() {
        String failureMessage = "failure message";

        underTest.reportFailure(stack, STACK_PATCH_TYPE, failureMessage);

        verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value.FAILURE, failureMessage);
    }

    @Test
    void affectedShouldNotFailWithNullValues() {
        underTest.reportAffected(null, null);

        verifyNullValues(UsageProto.CDPStackPatchEventType.Value.AFFECTED);
    }

    @Test
    void successShouldNotFailWithNullValues() {
        underTest.reportSuccess(null, null);

        verifyNullValues(UsageProto.CDPStackPatchEventType.Value.SUCCESS);
    }

    @Test
    void failureShouldNotFailWithNullValues() {
        underTest.reportFailure(null, null, null);

        verifyNullValues(UsageProto.CDPStackPatchEventType.Value.FAILURE);
    }

    private void verifyNonNullValues(UsageProto.CDPStackPatchEventType.Value eventType, String message) {
        Mockito.verify(usageReporter).cdpStackPatcherEvent(eventCaptor.capture());
        Assertions.assertThat(eventCaptor.getValue())
                .returns(RESOURCE_CRN, UsageProto.CDPStackPatchEvent::getResourceCrn)
                .returns(STACK_PATCH_TYPE.name(), UsageProto.CDPStackPatchEvent::getStackPatchType)
                .returns(eventType, UsageProto.CDPStackPatchEvent::getEventType)
                .returns(message, UsageProto.CDPStackPatchEvent::getMessage);
    }

    private void verifyNullValues(UsageProto.CDPStackPatchEventType.Value eventType) {
        Mockito.verify(usageReporter).cdpStackPatcherEvent(eventCaptor.capture());
        Assertions.assertThat(eventCaptor.getValue())
                .returns("", UsageProto.CDPStackPatchEvent::getResourceCrn)
                .returns(StackPatchType.UNKNOWN.name(), UsageProto.CDPStackPatchEvent::getStackPatchType)
                .returns(eventType, UsageProto.CDPStackPatchEvent::getEventType)
                .returns("", UsageProto.CDPStackPatchEvent::getMessage);
    }

}
