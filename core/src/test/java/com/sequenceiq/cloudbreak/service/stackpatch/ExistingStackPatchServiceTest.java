package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.repository.StackPatchRepository;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowRetryService;

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

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");
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

    static class NoopExistingStackPatchService extends ExistingStackPatchService {

        private boolean result = true;

        @Override
        public StackPatchType getStackPatchType() {
            return StackPatchType.UNKNOWN;
        }

        @Override
        public boolean isAffected(Stack stack) {
            return false;
        }

        @Override
        boolean doApply(Stack stack) {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }
    }

    static class ThrowExceptionExistingStackPatchService extends ExistingStackPatchService {

        @Override
        public StackPatchType getStackPatchType() {
            return StackPatchType.UNKNOWN;
        }

        @Override
        public boolean isAffected(Stack stack) {
            return false;
        }

        @Override
        boolean doApply(Stack stack) {
            throw new RuntimeException("Unexpected exception");
        }
    }

}