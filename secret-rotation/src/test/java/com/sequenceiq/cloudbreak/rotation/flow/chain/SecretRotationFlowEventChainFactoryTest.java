package com.sequenceiq.cloudbreak.rotation.flow.chain;

import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST;
import static com.sequenceiq.cloudbreak.rotation.common.TestSecretType.TEST_3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;

@ExtendWith(MockitoExtension.class)
public class SecretRotationFlowEventChainFactoryTest {

    @Mock
    private SecretRotationFlowEventProvider secretRotationFlowEventProvider;

    @InjectMocks
    private SecretRotationFlowEventChainFactory underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        FieldUtils.writeDeclaredField(underTest, "secretRotationFlowEventProviderOptional", Optional.of(secretRotationFlowEventProvider), true);
        lenient().when(secretRotationFlowEventProvider.getSaltUpdateTriggerEvent(any())).thenReturn(sampleEvent("salt"));
        when(secretRotationFlowEventProvider.saltUpdateNeeded(any())).thenCallRealMethod();
    }

    @Test
    void testFlowChainCounts() {
        when(secretRotationFlowEventProvider.getPostFlowEvent(any())).thenReturn(Set.of());
        // no salt update, no post flow
        assertEquals(1, underTest.createFlowTriggerEventQueue(
                new SecretRotationFlowChainTriggerEvent(null, 1L, null, List.of(TEST), null)).getQueue().size());
        // salt update, no post flow
        assertEquals(2, underTest.createFlowTriggerEventQueue(
                new SecretRotationFlowChainTriggerEvent(null, 1L, null, List.of(TEST_3), null)).getQueue().size());

        when(secretRotationFlowEventProvider.getPostFlowEvent(any())).thenReturn(Set.of(sampleEvent("post1")));
        // salt update, post flow
        assertEquals(3, underTest.createFlowTriggerEventQueue(
                new SecretRotationFlowChainTriggerEvent(null, 1L, null, List.of(TEST_3), null)).getQueue().size());
    }

    private static Selectable sampleEvent(String selector) {
        return new Selectable() {
            @Override
            public String selector() {
                return selector;
            }

            @Override
            public Long getResourceId() {
                return 1L;
            }
        };
    }
}
