package com.sequenceiq.cloudbreak.conclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStepResult;

class ConclusionCheckerTest {

    @Test
    public void doCheckShouldCallNextStepAfterFailedStep() {
        ConclusionStep firstStep = mock(ConclusionStep.class);
        when(firstStep.check(anyLong())).thenReturn(ConclusionStepResult.failed("failed"));
        ConclusionStep secondStep = mock(ConclusionStep.class);
        when(secondStep.check(anyLong())).thenReturn(ConclusionStepResult.failed("failed"));
        ConclusionChecker conclusionChecker = new ConclusionChecker(List.of(firstStep, secondStep));
        ConclusionResult conclusionResult = conclusionChecker.doCheck(1L);

        assertEquals(2, conclusionResult.getConclusions().size());
        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, times(1)).check(eq(1L));
    }

    @Test
    public void doCheckShouldStopAfterSuccessfulStep() {
        ConclusionStep firstStep = mock(ConclusionStep.class);
        when(firstStep.check(anyLong())).thenReturn(ConclusionStepResult.succeeded());
        ConclusionStep secondStep = mock(ConclusionStep.class);
        ConclusionChecker conclusionChecker = new ConclusionChecker(List.of(firstStep, secondStep));
        ConclusionResult conclusionResult = conclusionChecker.doCheck(1L);

        assertTrue(conclusionResult.getConclusions().isEmpty());
        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, never()).check(eq(1L));
    }

    @Test
    public void doCheckShouldThrowExceptionIfStepThrowsException() {
        ConclusionStep firstStep = mock(ConclusionStep.class);
        when(firstStep.check(anyLong())).thenThrow(new RuntimeException("Something wrong happened!"));
        ConclusionStep secondStep = mock(ConclusionStep.class);
        ConclusionChecker conclusionChecker = new ConclusionChecker(List.of(firstStep, secondStep));

        RuntimeException runtimeException = Assertions.assertThrows(RuntimeException.class, () -> conclusionChecker.doCheck(1L));

        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, never()).check(eq(1L));
        assertEquals("Something wrong happened!", runtimeException.getMessage());
    }
}