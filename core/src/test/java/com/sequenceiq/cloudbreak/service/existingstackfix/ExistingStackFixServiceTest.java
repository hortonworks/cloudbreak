package com.sequenceiq.cloudbreak.service.existingstackfix;

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
import com.sequenceiq.cloudbreak.domain.stack.StackFix;
import com.sequenceiq.cloudbreak.domain.stack.StackFix.StackFixType;
import com.sequenceiq.cloudbreak.repository.StackFixRepository;
import com.sequenceiq.flow.core.FlowLogService;

@ExtendWith(MockitoExtension.class)
class ExistingStackFixServiceTest {

    @Mock
    private StackFixRepository stackFixRepository;

    @Mock
    private FlowLogService flowLogService;

    @InjectMocks
    private NoopExistingStackFixService underTest;

    @Captor
    private ArgumentCaptor<StackFix> stackFixArgumentCaptor;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");
    }

    @Test
    void isStackAlreadyFixedFalse() {
        when(stackFixRepository.findByStackAndType(stack, StackFixType.UNKNOWN)).thenReturn(Optional.empty());

        boolean result = underTest.isStackAlreadyFixed(stack);

        assertThat(result).isFalse();
    }

    @Test
    void isStackAlreadyFixedTrue() {
        when(stackFixRepository.findByStackAndType(stack, StackFixType.UNKNOWN)).thenReturn(Optional.of(new StackFix()));

        boolean result = underTest.isStackAlreadyFixed(stack);

        assertThat(result).isTrue();
    }

    @Test
    void applyShouldFailWhenFlowIsRunning() {
        when(flowLogService.isOtherFlowRunning(stack.getId())).thenReturn(true);

        assertThatThrownBy(() -> underTest.apply(stack))
                .hasMessage("Another flow is running for stack stack-crn");
    }

    @Test
    void shouldSaveStackFixWhenApplyIsSuccessful() {
        underTest.apply(stack);

        verify(stackFixRepository).save(stackFixArgumentCaptor.capture());
        StackFix stackFix = stackFixArgumentCaptor.getValue();
        assertThat(stackFix)
                .returns(stack, StackFix::getStack)
                .returns(StackFixType.UNKNOWN, StackFix::getType);
    }

    static class NoopExistingStackFixService extends ExistingStackFixService {

        @Override
        public StackFixType getStackFixType() {
            return StackFixType.UNKNOWN;
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

}