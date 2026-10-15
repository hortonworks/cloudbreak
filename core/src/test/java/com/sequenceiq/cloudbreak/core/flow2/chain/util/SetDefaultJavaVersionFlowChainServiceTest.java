package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionTriggerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.javaversion.JavaVersionPolicy;

@ExtendWith(MockitoExtension.class)
class SetDefaultJavaVersionFlowChainServiceTest {

    private static final long STACK_ID = 1L;

    private static final String SOURCE_RUNTIME_VERSION = "7.3.1.600";

    @Mock
    private JavaVersionPolicy javaVersionPolicy;

    @InjectMocks
    private SetDefaultJavaVersionFlowChainService underTest;

    @Test
    void aTriggerEventIsCreatedWhenThePolicyReturnsAnAutoUpgradeTarget() {
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "7.3.2", 8)).thenReturn(OptionalInt.of(17));

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(8),
                SOURCE_RUNTIME_VERSION, "7.3.2");

        assertThat(events).hasSize(1);
        SetDefaultJavaVersionTriggerEvent javaEvent = events.getFirst();
        assertEquals(SET_DEFAULT_JAVA_VERSION_CHAIN_TRIGGER_EVENT, javaEvent.selector());
        assertEquals(STACK_ID, javaEvent.getResourceId());
        assertEquals("17", javaEvent.getDefaultJavaVersion());
    }

    @Test
    void noTriggerEventIsCreatedWhenThePolicyKeepsTheCurrentJavaVersion() {
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "7.3.3.0", 17)).thenReturn(OptionalInt.empty());

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(17),
                SOURCE_RUNTIME_VERSION, "7.3.3.0");

        assertThat(events).isEmpty();
    }

    @Test
    void theRuntimeTheClusterIsUpgradedFromIsPassedToThePolicy() {
        when(javaVersionPolicy.autoUpgradeTarget(SOURCE_RUNTIME_VERSION, "7.3.3.0", 17)).thenReturn(OptionalInt.of(21));

        List<SetDefaultJavaVersionTriggerEvent> events = underTest.setDefaultJavaVersionTriggerEvent(stackDto(17),
                SOURCE_RUNTIME_VERSION, "7.3.3.0");

        assertThat(events).singleElement().satisfies(event -> assertEquals("21", event.getDefaultJavaVersion()));
    }

    private StackDto stackDto(int currentJavaVersion) {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        Stack stack = mock(Stack.class);
        when(stack.getJavaVersion()).thenReturn(currentJavaVersion);
        when(stackDto.getStack()).thenReturn(stack);
        return stackDto;
    }
}
