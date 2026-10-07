package com.sequenceiq.stackpatcher.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.stackpatcher.TestStackPatchResource;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.repository.StackPatchRepository;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatchServiceTest {

    @Mock
    private StackPatchRepository stackPatchRepository;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private FlowRetryService flowRetryService;

    @InjectMocks
    private NoopExistingStackPatchService underTest;

    @InjectMocks
    private ThrowExceptionExistingStackPatchService alsoUnderTest;

    @Captor
    private ArgumentCaptor<StackPatch> stackPatchArgumentCaptor;

    private TestStackPatchResource stack;

    @BeforeEach
    void setUp() {
        stack = new TestStackPatchResource(123L, "stack-crn");
    }

    @Test
    void applyShouldNotSucceedWhenFlowIsRunning() throws ExistingStackPatchApplyException {
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(true);

        boolean result = underTest.apply(stack);

        assertThat(result).isFalse();
    }

    @Test
    void applyShouldNotSucceedWhenLastFlowRetryable() throws ExistingStackPatchApplyException {
        when(flowRetryService.getLastRetryableFailedFlow(stack.getId())).thenReturn(Optional.of(new FlowLog()));

        boolean result = underTest.apply(stack);

        assertThat(result).isFalse();
    }

    @Test
    void applyShouldSucceedWhenLastFlowRetryableButItShouldNotCheck() throws ExistingStackPatchApplyException {
        lenient().when(flowRetryService.getLastRetryableFailedFlow(stack.getId())).thenReturn(Optional.of(new FlowLog()));
        underTest.setShouldCheckForFailedRetryableFlow(false);

        boolean result = underTest.apply(stack);

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotSaveStackPatchWhenApplyDoesNotSucceed() throws ExistingStackPatchApplyException {
        underTest.setResult(false);

        underTest.apply(stack);

        verifyNoInteractions(stackPatchRepository);
        // reset result
        underTest.setResult(true);
    }

    @Test
    void shouldTranslateUnexpectedExceptionType() throws ExistingStackPatchApplyException {
        assertThatThrownBy(() -> alsoUnderTest.apply(stack))
                .isInstanceOf(ExistingStackPatchApplyException.class)
                .hasMessage("Something unexpected went wrong with stack %s while applying patch %s: Unexpected exception",
                        stack.getResourceCrn(), StackPatchType.UNKNOWN);
    }

    static class NoopExistingStackPatchService extends ExistingStackPatchService<TestStackPatchResource> {

        private boolean result = true;

        private boolean shouldCheckForFailedRetryableFlow = true;

        @Override
        public StackPatchType getStackPatchType() {
            return StackPatchType.UNKNOWN;
        }

        @Override
        public boolean isAffected(TestStackPatchResource stack) {
            return false;
        }

        @Override
        protected boolean doApply(TestStackPatchResource stack) {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }

        @Override
        protected boolean shouldCheckForFailedRetryableFlow() {
            return shouldCheckForFailedRetryableFlow;
        }

        public void setShouldCheckForFailedRetryableFlow(boolean shouldCheckForFailedRetryableFlow) {
            this.shouldCheckForFailedRetryableFlow = shouldCheckForFailedRetryableFlow;
        }
    }

    static class ThrowExceptionExistingStackPatchService extends ExistingStackPatchService<TestStackPatchResource> {

        @Override
        public StackPatchType getStackPatchType() {
            return StackPatchType.UNKNOWN;
        }

        @Override
        public boolean isAffected(TestStackPatchResource stack) {
            return false;
        }

        @Override
        protected boolean doApply(TestStackPatchResource stack) {
            throw new RuntimeException("Unexpected exception");
        }
    }

}