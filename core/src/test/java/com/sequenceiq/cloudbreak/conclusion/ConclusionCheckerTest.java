package com.sequenceiq.cloudbreak.conclusion;

import static com.sequenceiq.cloudbreak.conclusion.ConclusionStepNode.stepNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.conclusion.step.Conclusion;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.NodeServicesCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.SaltCheckerConclusionStep;
import com.sequenceiq.cloudbreak.conclusion.step.VmStatusCheckerConclusionStep;

class ConclusionCheckerTest {

    @Test
    public void doCheckShouldStopStepAfterFailedStep() {
        ConclusionStep firstStep = mock(SaltCheckerConclusionStep.class);
        when(firstStep.check(anyLong())).thenReturn(Conclusion.failed("failed", "details", ConclusionStep.class));
        ConclusionStep secondStep = mock(NodeServicesCheckerConclusionStep.class);
        ConclusionStepNode stepNode = stepNode(SaltCheckerConclusionStep.class)
                .withSuccessNode(stepNode(NodeServicesCheckerConclusionStep.class));
        ConclusionChecker conclusionChecker = new ConclusionChecker(stepNode,
                Map.of(SaltCheckerConclusionStep.class, firstStep, NodeServicesCheckerConclusionStep.class, secondStep));
        ConclusionResult conclusionResult = conclusionChecker.doCheck(1L);

        assertEquals(1, conclusionResult.getConclusions().size());
        assertEquals(1, conclusionResult.getFailedConclusionTexts().size());
        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, never()).check(eq(1L));
    }

    @Test
    public void doCheckShouldStopAfterSuccessfulStep() {
        ConclusionStep firstStep = mock(SaltCheckerConclusionStep.class);
        when(firstStep.check(anyLong())).thenReturn(Conclusion.succeeded(ConclusionStep.class));
        ConclusionStep secondStep = mock(NodeServicesCheckerConclusionStep.class);
        ConclusionStepNode stepNode = stepNode(SaltCheckerConclusionStep.class)
                .withFailureNode(stepNode(VmStatusCheckerConclusionStep.class));
        ConclusionChecker conclusionChecker = new ConclusionChecker(stepNode,
                Map.of(SaltCheckerConclusionStep.class, firstStep, NodeServicesCheckerConclusionStep.class, secondStep));
        ConclusionResult conclusionResult = conclusionChecker.doCheck(1L);

        assertEquals(1, conclusionResult.getConclusions().size());
        assertTrue(conclusionResult.getFailedConclusionTexts().isEmpty());
        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, never()).check(eq(1L));
    }

    @Test
    public void doCheckShouldCallNextAfterSuccessfulStep() {
        ConclusionStep firstStep = mock(SaltCheckerConclusionStep.class);
        when(firstStep.check(anyLong())).thenReturn(Conclusion.succeeded(ConclusionStep.class));
        ConclusionStep secondStep = mock(ConclusionStep.class);
        when(secondStep.check(anyLong())).thenReturn(Conclusion.succeeded(ConclusionStep.class));
        ConclusionStepNode stepNode = stepNode(SaltCheckerConclusionStep.class)
                .withSuccessNode(stepNode(NodeServicesCheckerConclusionStep.class));
        ConclusionChecker conclusionChecker = new ConclusionChecker(stepNode,
                Map.of(SaltCheckerConclusionStep.class, firstStep, NodeServicesCheckerConclusionStep.class, secondStep));
        ConclusionResult conclusionResult = conclusionChecker.doCheck(1L);

        assertEquals(2, conclusionResult.getConclusions().size());
        assertTrue(conclusionResult.getFailedConclusionTexts().isEmpty());
        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, times(1)).check(eq(1L));
    }

    @Test
    public void doCheckShouldThrowExceptionIfStepThrowsException() {
        ConclusionStep firstStep = mock(SaltCheckerConclusionStep.class);
        when(firstStep.check(anyLong())).thenThrow(new RuntimeException("Something wrong happened!"));
        ConclusionStep secondStep = mock(NodeServicesCheckerConclusionStep.class);
        ConclusionStepNode stepNode = stepNode(SaltCheckerConclusionStep.class)
                .withSuccessNode(stepNode(NodeServicesCheckerConclusionStep.class));
        ConclusionChecker conclusionChecker = new ConclusionChecker(stepNode,
                Map.of(SaltCheckerConclusionStep.class, firstStep, NodeServicesCheckerConclusionStep.class, secondStep));

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> conclusionChecker.doCheck(1L));

        verify(firstStep, times(1)).check(eq(1L));
        verify(secondStep, never()).check(eq(1L));
        assertEquals("Something wrong happened!", runtimeException.getMessage());
    }
}