package com.sequenceiq.stackpatcher.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherServiceProviderTest {

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.MOCK;

    @InjectMocks
    private ExistingStackPatcherServiceProvider underTest;

    @Spy
    private Collection<ExistingStackPatchService<?>> existingStackPatchServices = new HashSet<>();

    @Mock
    private ExistingStackPatchService<?> existingStackPatchService;

    @BeforeEach
    void setUp() {
        lenient().when(existingStackPatchService.getStackPatchType()).thenReturn(STACK_PATCH_TYPE);
        existingStackPatchServices.clear();
        existingStackPatchServices.add(existingStackPatchService);
    }

    @Test
    void shouldFailWithNullName() {
        String stackPatchTypeName = null;
        assertThatThrownBy(() -> underTest.provide(stackPatchTypeName))
                .isInstanceOf(UnknownStackPatchTypeException.class)
                .hasMessage("Could not find stack patcher service for type " + stackPatchTypeName);
    }

    @Test
    void shouldFailWithUnknownName() {
        String stackPatchTypeName = "UNKNOWN_TYPE";
        assertThatThrownBy(() -> underTest.provide(stackPatchTypeName))
                .isInstanceOf(UnknownStackPatchTypeException.class)
                .hasMessage("Could not find stack patcher service for type " + stackPatchTypeName);
    }

    @Test
    void shouldReturnServiceWithValidName() throws UnknownStackPatchTypeException {
        String stackPatchTypeName = STACK_PATCH_TYPE.name();

        ExistingStackPatchService<?> stackPatchService = underTest.provide(stackPatchTypeName);

        assertThat(stackPatchService).isEqualTo(this.existingStackPatchService);
    }

    @Test
    void shouldFailWithTypeWithoutService() {
        StackPatchType stackPatchType = StackPatchType.TEST_PATCH_1;
        assertThatThrownBy(() -> underTest.provide(stackPatchType))
                .isInstanceOf(UnknownStackPatchTypeException.class)
                .hasMessage("Could not find stack patcher service for type " + stackPatchType);
    }

    @Test
    void shouldReturnServiceWithValidType() throws UnknownStackPatchTypeException {
        ExistingStackPatchService<?> stackPatchService = underTest.provide(STACK_PATCH_TYPE);

        assertThat(stackPatchService).isEqualTo(this.existingStackPatchService);
    }

}
