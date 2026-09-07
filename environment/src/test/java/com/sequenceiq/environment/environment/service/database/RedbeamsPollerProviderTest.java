package com.sequenceiq.environment.environment.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.poller.FlowResultPollerEvaluator;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.RedbeamsOperationFailedException;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v1.RedBeamsFlowEndpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerProviderTest {

    private static final Long ENV_ID = 1L;

    private static final String USER_CRN = "userCrn";

    private static final String DB_CRN_1 = "dbCrn1";

    private static final String DB_CRN_2 = "dbCrn2";

    private static final Map<String, String> TAGS = Map.of("custom", "value");

    private static final Set<String> TAG_KEYS = Set.of("custom");

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

    @ParameterizedTest
    @MethodSource("updateUserDefinedTagsSource")
    void testUpdateUserDefinedTags(boolean hasActiveFlow, boolean latestFlowFinalizedAndFailed, AttemptState expectedResult) {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flow-1");
        FlowCheckResponse flowCheckResponse = mock(FlowCheckResponse.class);
        when(flowCheckResponse.getHasActiveFlow()).thenReturn(hasActiveFlow);
        lenient().when(flowCheckResponse.getLatestFlowFinalizedAndFailed()).thenReturn(latestFlowFinalizedAndFailed);
        when(redbeamsService.checkFlow(flowIdentifier)).thenReturn(flowCheckResponse);

        try (MockedStatic<ThreadBasedUserCrnProvider> mockedCrn = Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            mockedCrn.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class)))
                    .thenAnswer(invocation -> {
                        Supplier<Object> supplier = invocation.getArgument(0);
                        return supplier.get();
                    });

            AttemptResult<Void> result = underTest.updateUserDefinedTags(ENV_ID, flowIdentifier);

            assertEquals(expectedResult, result.getState());
        }

    }

    private static Stream<Arguments> updateUserDefinedTagsSource() {
        return Stream.of(
                Arguments.of(false, false, AttemptState.FINISH),
                Arguments.of(true, false, AttemptState.CONTINUE),
                Arguments.of(false, true, AttemptState.BREAK)
        );
    }

    @Test
    void testUserDefinedTagsDeletePoller() throws Exception {
        AttemptResult<List<FlowIdentifier>> finishedResult = AttemptResults.finishWith(List.of());
        when(flowResultPollerEvaluator.attemptResultFinisher(any())).thenReturn(finishedResult);

        AttemptMaker<List<FlowIdentifier>> attemptMaker = underTest.userDefinedTagsDeletePoller(List.of(DB_CRN_1, DB_CRN_2), ENV_ID, TAG_KEYS);
        AttemptResult<List<FlowIdentifier>> result = attemptMaker.process();

        assertNotNull(result);
        verify(redbeamsService, times(2)).triggerUserDefinedTagsDelete(any(), eq(TAG_KEYS));
        verify(redbeamsService).triggerUserDefinedTagsDelete(DB_CRN_1, TAG_KEYS);
        verify(redbeamsService).triggerUserDefinedTagsDelete(DB_CRN_2, TAG_KEYS);
    }

    @Test
    void testUserDefinedTagsDeletePollerRetainsFlowIdentifiersAcrossAttempts() throws Exception {
        FlowIdentifier flow1 = new FlowIdentifier(FlowType.FLOW, "flow-1");
        FlowIdentifier flow2 = new FlowIdentifier(FlowType.FLOW, "flow-2");

        when(redbeamsService.triggerUserDefinedTagsDelete(DB_CRN_1, TAG_KEYS)).thenReturn(flow1);
        when(redbeamsService.triggerUserDefinedTagsDelete(DB_CRN_2, TAG_KEYS))
                .thenThrow(flowRunningConflict())
                .thenReturn(flow2);
        stubFlowResultPollerEvaluator();

        AttemptMaker<List<FlowIdentifier>> attemptMaker =
                underTest.userDefinedTagsDeletePoller(List.of(DB_CRN_1, DB_CRN_2), ENV_ID, TAG_KEYS);

        AttemptResult<List<FlowIdentifier>> firstResult = attemptMaker.process();
        assertEquals(AttemptState.CONTINUE, firstResult.getState());

        AttemptResult<List<FlowIdentifier>> secondResult = attemptMaker.process();
        assertEquals(AttemptState.FINISH, secondResult.getState());
        assertEquals(List.of(flow1, flow2), secondResult.getResult());
        verify(redbeamsService, times(1)).triggerUserDefinedTagsDelete(DB_CRN_1, TAG_KEYS);
        verify(redbeamsService, times(2)).triggerUserDefinedTagsDelete(DB_CRN_2, TAG_KEYS);
    }

    @Test
    void testUserDefinedTagsDeletePollerRetriesWhenServiceWrapsConflict() throws Exception {
        DatabaseServerV4Endpoint databaseServerV4Endpoint = mock(DatabaseServerV4Endpoint.class);
        RedBeamsService realRedbeamsService = new RedBeamsService(databaseServerV4Endpoint, mock(SupportV4Endpoint.class),
                mock(RedBeamsFlowEndpoint.class), new WebApplicationExceptionMessageExtractor());
        RedbeamsPollerProvider providerWithRealService = new RedbeamsPollerProvider(realRedbeamsService, flowResultPollerEvaluator, mock(StackService.class));
        stubFlowResultPollerEvaluator();
        when(databaseServerV4Endpoint.deleteUserDefinedTags(eq(DB_CRN_1), any()))
                .thenThrow(new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));

        AttemptResult<List<FlowIdentifier>> result = providerWithRealService.userDefinedTagsDeletePoller(List.of(DB_CRN_1), ENV_ID, TAG_KEYS).process();

        assertEquals(AttemptState.CONTINUE, result.getState());
    }

    private void stubFlowResultPollerEvaluator() {
        when(flowResultPollerEvaluator.attemptResultFinisher(any())).thenAnswer(invocation -> {
            List<AttemptResult<FlowIdentifier>> results = invocation.getArgument(0);
            if (results.stream().anyMatch(r -> r.getState() == AttemptState.BREAK)) {
                return AttemptResults.breakFor(new RuntimeException());
            }
            if (results.stream().anyMatch(r -> r.getState() == AttemptState.CONTINUE)) {
                return AttemptResults.justContinue();
            }
            return AttemptResults.finishWith(results.stream().map(AttemptResult::getResult).toList());
        });
    }

    private static RedbeamsOperationFailedException flowRunningConflict() {
        return new RedbeamsOperationFailedException("Database has flow running already",
                new WebApplicationException(Response.status(Response.Status.CONFLICT).build()));
    }
}