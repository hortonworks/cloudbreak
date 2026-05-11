package com.sequenceiq.environment.environment.service.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.poller.FlowResultPollerEvaluator;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerProviderTest {

    private static final Long ENV_ID = 1L;

    private static final String USER_CRN = "userCrn";

    private static final String DB_CRN_1 = "dbCrn1";

    private static final String DB_CRN_2 = "dbCrn2";

    private static final Map<String, String> TAGS = Map.of("custom", "value");

    @Mock
    private RedBeamsService redbeamsService;

    @Mock
    private FlowResultPollerEvaluator flowResultPollerEvaluator;

    @InjectMocks
    private RedbeamsPollerProvider underTest;

    @Test
    void testUserDefinedTagsUpdatePoller() throws Exception {
        AttemptResult<List<FlowIdentifier>> finishedResult = AttemptResults.finishWith(List.of());
        when(flowResultPollerEvaluator.attemptResultFinisher(any())).thenReturn(finishedResult);

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedCrn = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedCrn.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);

            AttemptMaker<List<FlowIdentifier>> attemptMaker = underTest.userDefinedTagsUpdatePoller(List.of(DB_CRN_1, DB_CRN_2), ENV_ID, TAGS);
            AttemptResult<List<FlowIdentifier>> result = attemptMaker.process();

            assertNotNull(result);
            verify(redbeamsService, times(2)).triggerUserDefinedTagsUpdate(any(), eq(TAGS));
            verify(redbeamsService).triggerUserDefinedTagsUpdate(DB_CRN_1, TAGS);
            verify(redbeamsService).triggerUserDefinedTagsUpdate(DB_CRN_2, TAGS);
        }
    }

    @Test
    void testUserDefinedTagsUpdatePollerWhenFlowAlreadyRunning() throws Exception {
        AttemptResult<List<FlowIdentifier>> continueResult = AttemptResults.justContinue();
        when(flowResultPollerEvaluator.attemptResultFinisher(any())).thenReturn(continueResult);
        doThrow(new BadRequestException("Flow already running")).when(redbeamsService).triggerUserDefinedTagsUpdate(DB_CRN_1, TAGS);

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedCrn = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedCrn.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);

            AttemptMaker<List<FlowIdentifier>> attemptMaker = underTest.userDefinedTagsUpdatePoller(List.of(DB_CRN_1), ENV_ID, TAGS);

            attemptMaker.process();
            verify(redbeamsService, times(1)).triggerUserDefinedTagsUpdate(DB_CRN_1, TAGS);

            attemptMaker.process();
            verify(redbeamsService, times(2)).triggerUserDefinedTagsUpdate(DB_CRN_1, TAGS);
        }
    }

    @Test
    void testUserDefinedTagsUpdatePollerWhenExceptionOccurs() throws Exception {
        AttemptResult<List<FlowIdentifier>> breakResult = AttemptResults.breakFor(new RuntimeException("Unexpected"));
        when(flowResultPollerEvaluator.attemptResultFinisher(any())).thenReturn(breakResult);
        doThrow(new RuntimeException("Unexpected error")).when(redbeamsService).triggerUserDefinedTagsUpdate(DB_CRN_1, TAGS);

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedCrn = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedCrn.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);

            AttemptMaker<List<FlowIdentifier>> attemptMaker = underTest.userDefinedTagsUpdatePoller(List.of(DB_CRN_1), ENV_ID, TAGS);
            AttemptResult<List<FlowIdentifier>> result = attemptMaker.process();

            assertNotNull(result);
            verify(redbeamsService).triggerUserDefinedTagsUpdate(DB_CRN_1, TAGS);
            verify(flowResultPollerEvaluator).attemptResultFinisher(any());
        }
    }
}