package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
    void isStackAlreadyFixedFalse() {
        when(stackPatchRepository.findByStackAndType(stack, StackPatchType.UNKNOWN)).thenReturn(Optional.empty());

        boolean result = underTest.isStackAlreadyFixed(stack);

        assertThat(result).isFalse();
    }

    @Test
    void isStackAlreadyFixedTrue() {
        when(stackPatchRepository.findByStackAndType(stack, StackPatchType.UNKNOWN)).thenReturn(Optional.of(new StackPatch()));

        boolean result = underTest.isStackAlreadyFixed(stack);

        assertThat(result).isTrue();
    }

    @Test
    void applyShouldFailWhenFlowIsRunning() {
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(true);

        assertThatThrownBy(() -> underTest.apply(stack))
                .hasMessage("Another flow is running for stack %s, skipping patch apply to let the flow finish", stack.getResourceCrn());
    }

    @Test
    void applyShouldFailWhenLastFlowRetryable() {
        when(flowRetryService.getLastRetryableFailedFlow(stack.getId())).thenReturn(Optional.of(new FlowLog()));

        assertThatThrownBy(() -> underTest.apply(stack))
                .hasMessage("Stack %s has a retryable failed flow, skipping patch apply to preserve possible retry", stack.getResourceCrn());
    }

    @Test
    void shouldSaveStackPatchWhenApplyIsSuccessful() throws ExistingStackPatchApplyException {
        underTest.apply(stack);

        verify(stackPatchRepository).save(stackPatchArgumentCaptor.capture());
        StackPatch stackPatch = stackPatchArgumentCaptor.getValue();
        assertThat(stackPatch)
                .returns(stack, StackPatch::getStack)
                .returns(StackPatchType.UNKNOWN, StackPatch::getType);
    }

    @Test
    void shouldTranslateUnexpectedExceptionType() throws ExistingStackPatchApplyException {
        assertThatThrownBy(() -> alsoUnderTest.apply(stack))
                .isInstanceOf(ExistingStackPatchApplyException.class)
                .hasMessage("Something unexpected went wrong with stack %s while applying patch %s",
                        stack.getResourceCrn(), StackPatchType.UNKNOWN);
    }

    static class NoopExistingStackPatchService extends ExistingStackPatchService {

        @Override
        public StackPatchType getStackPatchType() {
            return StackPatchType.UNKNOWN;
        }

        @Override
        public boolean isAffected(Stack stack) {
            return false;
        }

        @Override
        void doApply(Stack stack) {
            // do nothing
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
        void doApply(Stack stack) {
            throw new RuntimeException("Unexpected exception");
        }
    }

}