package com.sequenceiq.freeipa.job.stackpatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.freeipa.entity.StackPatchType;
import com.sequenceiq.freeipa.service.stackpatch.ExistingStackPatchService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherServiceProviderTest {

    @Mock
    private ExistingStackPatchService minifiPatchService;

    private ExistingStackPatcherServiceProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new ExistingStackPatcherServiceProvider();
        ReflectionTestUtils.setField(underTest, "existingStackPatchServices", List.of(minifiPatchService));
    }

    @Test
    void provideByNameReturnsMatchingService() throws UnknownStackPatchTypeException {
        when(minifiPatchService.getStackPatchType()).thenReturn(StackPatchType.MINIFI_RETRY_CONFIG_FIX);

        assertEquals(minifiPatchService, underTest.provide("MINIFI_RETRY_CONFIG_FIX"));
    }

    @Test
    void provideByUnknownNameThrows() {
        assertThrows(UnknownStackPatchTypeException.class, () -> underTest.provide("NOT_A_REAL_TYPE"));
    }

    @Test
    void provideByTypeThrowsWhenNoImplementationFound() {
        underTest = new ExistingStackPatcherServiceProvider();
        ReflectionTestUtils.setField(underTest, "existingStackPatchServices", List.of());

        assertThrows(UnknownStackPatchTypeException.class, () -> underTest.provide(StackPatchType.MINIFI_RETRY_CONFIG_FIX));
    }
}
