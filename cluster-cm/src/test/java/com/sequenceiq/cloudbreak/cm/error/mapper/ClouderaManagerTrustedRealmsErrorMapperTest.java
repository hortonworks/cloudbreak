package com.sequenceiq.cloudbreak.cm.error.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerTrustedRealmsErrorMapperTest {

    private static final long STACK_ID = 1L;

    private static final String MESSAGE = "message";

    @InjectMocks
    private ClouderaManagerTrustedRealmsErrorMapper underTest;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private Blueprint blueprint;

    @Mock
    private CommandDetails commandDetails;

    @BeforeEach
    void setUp() {
        when(stack.getBlueprint()).thenReturn(blueprint);
    }

    @Test
    void nonHybridStack() {
        when(blueprint.getHybridOption()).thenReturn(null);

        boolean result = underTest.canHandle(stack, List.of(commandDetails));

        assertThat(result).isFalse();
    }

    @Test
    void hybridStackFailedForOtherReason() {
        when(blueprint.getHybridOption()).thenReturn(BlueprintHybridOption.BURST_TO_CLOUD);
        when(commandDetails.getName()).thenReturn("OtherCommand");

        boolean result = underTest.canHandle(stack, List.of(commandDetails));

        assertThat(result).isFalse();
    }

    @Test
    void hybridStackFailedForRangerAdminCreateRepo() {
        when(stack.getStackVersion()).thenReturn("7.3.1");
        when(blueprint.getHybridOption()).thenReturn(BlueprintHybridOption.BURST_TO_CLOUD);
        when(commandDetails.getName()).thenReturn("RangerAdmin");

        boolean result = underTest.canHandle(stack, List.of(commandDetails));
        assertThat(result).isTrue();

        String mappedMessage = underTest.map(stack, List.of(commandDetails), MESSAGE);
        assertThat(mappedMessage).isEqualTo("Failed to create Ranger resource policies. "
                + "Please verify that trusted realms are configured correctly in On-Premises Environment: "
                + DocumentationLinkProvider.hybridSetupTrustedRealmsLink(stack.getStackVersion()));
    }

}
