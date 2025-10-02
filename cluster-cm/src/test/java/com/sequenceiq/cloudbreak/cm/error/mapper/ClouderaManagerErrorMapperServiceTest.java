package com.sequenceiq.cloudbreak.cm.error.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerErrorMapperServiceTest {

    private static final String MESSAGE = "message";

    @Mock
    private ClouderaManagerErrorMapper mapper1;

    @Mock
    private ClouderaManagerErrorMapper mapper2;

    private ClouderaManagerErrorMapperService underTest;

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private Blueprint blueprint;

    @Mock
    private CommandDetails commandDetails;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getBlueprint()).thenReturn(blueprint);
        underTest = new ClouderaManagerErrorMapperService();
        ReflectionTestUtils.setField(underTest, "clouderaManagerErrorMappers", List.of(mapper1, mapper2));
    }

    @Test
    void noMappers() {
        String result = underTest.map(stack, List.of(commandDetails), MESSAGE);

        assertThat(result).isEqualTo(MESSAGE);
        verify(mapper1).canHandle(stack, List.of(commandDetails));
        verify(mapper2).canHandle(stack, List.of(commandDetails));
    }

    @Test
    void secondMapperCanHandle() {
        when(mapper2.canHandle(stack, List.of(commandDetails))).thenReturn(true);
        when(mapper2.map(stack, List.of(commandDetails), MESSAGE)).thenReturn("mappedmessage");

        String result = underTest.map(stack, List.of(commandDetails), MESSAGE);

        assertThat(result).isEqualTo("mappedmessage");
        verify(mapper1).canHandle(stack, List.of(commandDetails));
    }

    @Test
    void mapperFailsToHandle() {
        when(mapper2.canHandle(stack, List.of(commandDetails))).thenReturn(true);
        when(mapper2.map(stack, List.of(commandDetails), MESSAGE)).thenThrow(RuntimeException.class);

        String result = underTest.map(stack, List.of(commandDetails), MESSAGE);

        assertThat(result).isEqualTo(MESSAGE);
    }

}
