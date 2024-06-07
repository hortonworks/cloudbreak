package com.sequenceiq.cloudbreak.core.flow2.stack.provision;

import static com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState.IMAGE_CHECK_STATE;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;

class StackCreationStateTest {

    private static final Map<StackCreationState, String> ENUM_TO_ACTION_MAP = new EnumMap<>(Map.ofEntries(
            entry(IMAGE_CHECK_STATE, "com.sequenceiq.cloudbreak.core.flow2.stack.provision.action.CheckImageAction")
    ));

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(StackCreationState.class)
    void actionTest(StackCreationState underTest) {
        Class<? extends AbstractStackAction<?, ?, ?, ?>> action = underTest.action();

        assertThat(action == null ? null : action.getName()).isEqualTo(ENUM_TO_ACTION_MAP.get(underTest));
    }
}