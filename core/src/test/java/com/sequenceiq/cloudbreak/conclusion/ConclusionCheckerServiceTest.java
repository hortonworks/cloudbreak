package com.sequenceiq.cloudbreak.conclusion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.conclusion.step.Conclusion;
import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.redbeams.api.model.common.Status;

@ExtendWith(MockitoExtension.class)
class ConclusionCheckerServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @Mock
    private ConclusionCheckerFactory conclusionCheckerFactory;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private ConclusionCheckerService underTest;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testRunConclusionChecker(boolean conclusionFound) {
        ConclusionChecker conclusionChecker = mock(ConclusionChecker.class);
        ConclusionResult conclusionResult = conclusionFound
                ? new ConclusionResult(List.of(Conclusion.failed("error", "details", ConclusionStep.class)))
                : new ConclusionResult(List.of());
        when(conclusionChecker.doCheck(eq(STACK_ID))).thenReturn(conclusionResult);
        when(conclusionCheckerFactory.getConclusionChecker(eq(ConclusionCheckerType.DEFAULT))).thenReturn(conclusionChecker);
        when(entitlementService.conclusionCheckerSendUserEventEnabled(anyString())).thenReturn(Boolean.TRUE);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.runConclusionChecker(STACK_ID, Status.STOP_FAILED.name(), ResourceEvent.CLUSTER_STOP_FAILED,
                        ConclusionCheckerType.DEFAULT, "eventMessageArg"));

        verify(conclusionChecker, times(1)).doCheck(eq(STACK_ID));
        verifyNoMoreInteractions(conclusionChecker);
        VerificationMode verificationMode = conclusionFound ? times(1) : never();
        verify(flowMessageService, verificationMode).fireEventAndLog(eq(STACK_ID), eq(Status.STOP_FAILED.name()), eq(ResourceEvent.CLUSTER_STOP_FAILED),
                eq("eventMessageArg"), eq("[error]"));
    }

    @Test
    public void testWhenConclusionCheckerFailed() {
        ConclusionChecker conclusionChecker = mock(ConclusionChecker.class);
        when(conclusionCheckerFactory.getConclusionChecker(eq(ConclusionCheckerType.DEFAULT))).thenReturn(conclusionChecker);
        when(conclusionChecker.doCheck(eq(STACK_ID))).thenThrow(new RuntimeException("error"));

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN,
                () -> underTest.runConclusionChecker(STACK_ID, Status.STOP_FAILED.name(), ResourceEvent.CLUSTER_STOP_FAILED, ConclusionCheckerType.DEFAULT,
                        "eventMessageArg"));

        verify(conclusionChecker, times(1)).doCheck(eq(STACK_ID));
        verifyNoMoreInteractions(conclusionChecker);
        verify(flowMessageService, never()).fireEventAndLog(anyLong(), anyString(), any(), anyString(), anyString());
    }
}