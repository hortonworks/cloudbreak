package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private Runnable runnable;

    @Mock
    private Supplier<Boolean> supplier;

    @InjectMocks
    private RetryService undertest;

    @Test
    void actionIsCalledWhenWrappingRte() {
        undertest.testWith2SecDelayMax5Times(Retry.ActionFailedException.wrapRte(runnable));
        verify(runnable, times(1)).run();
    }

    @Test
    void rteIsConvertedToActionFailedException() {
        doThrow(RuntimeException.class).when(runnable).run();
        assertThrows(Retry.ActionFailedException.class, () -> undertest.testWith2SecDelayMax5Times(Retry.ActionFailedException.wrapRte(runnable)));
        verify(runnable, times(1)).run();
    }

    @Test
    void actionIsCalledWhenWrappingRteSupplier() {
        undertest.testWith2SecDelayMax15Times(Retry.ActionFailedException.wrapRte(supplier));
        verify(supplier, times(1)).get();
    }

    @Test
    void rteIsConvertedToActionFailedExceptionSupplier() {
        doThrow(RuntimeException.class).when(supplier).get();
        assertThrows(Retry.ActionFailedException.class, () -> undertest.testWith2SecDelayMax15Times(Retry.ActionFailedException.wrapRte(supplier)));
        verify(supplier, times(1)).get();
    }

}