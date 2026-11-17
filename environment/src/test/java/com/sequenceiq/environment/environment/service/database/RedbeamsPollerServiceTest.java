package com.sequenceiq.environment.environment.service.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.exception.StackOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;
import com.sequenceiq.redbeams.api.model.common.Status;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerServiceTest {

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "envCrn";

    private static final String DB_CRN = "dbCrn";

    private static final Map<String, String> TAGS = Map.of("custom", "value");

    private static final Set<String> TAG_KEYS = Set.of("custom");

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private RedbeamsPollerProvider redbeamsPollerProvider;

    @InjectMocks
    private RedbeamsPollerService underTest;

    @Test
    void testUpdateUserDefinedTags() {
        setPollingConfig(5, 1);

        DatabaseServerV4Response dbResponse = new DatabaseServerV4Response();
        dbResponse.setCrn(DB_CRN);
        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(Set.of(dbResponse));

        FlowIdentifier expectedFlow = new FlowIdentifier(FlowType.FLOW, "flow-id-1");
        AttemptMaker<List<FlowIdentifier>> triggerAttemptMaker = () -> AttemptResults.finishWith(List.of(expectedFlow));
        AttemptMaker<Void> completionAttemptMaker = () -> AttemptResults.finishWith(null);

        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS)).thenReturn(triggerAttemptMaker);
        when(redbeamsPollerProvider.userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID)).thenReturn(completionAttemptMaker);

        underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS, null);

        verify(databaseServerV4Endpoint).list(ENV_CRN);
        verify(redbeamsPollerProvider).userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS);
        verify(redbeamsPollerProvider).userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID);
    }

    @Test
    void testUpdateUserDefinedTagsFailsWhenFlowFails() {
        setPollingConfig(5, 1);

        DatabaseServerV4Response dbResponse = new DatabaseServerV4Response();
        dbResponse.setCrn(DB_CRN);
        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(Set.of(dbResponse));

        FlowIdentifier expectedFlow = new FlowIdentifier(FlowType.FLOW, "flow-id-1");
        AttemptMaker<List<FlowIdentifier>> triggerAttemptMaker = () -> AttemptResults.finishWith(List.of(expectedFlow));
        AttemptMaker<Void> completionAttemptMaker = () -> AttemptResults.breakFor("Update user defined tags on DB stack failed.");

        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS)).thenReturn(triggerAttemptMaker);
        when(redbeamsPollerProvider.userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID)).thenReturn(completionAttemptMaker);

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS, null));
    }

    @Test
    void testUpdateUserDefinedTagsWhenPollerTimedOut() {
        setPollingConfig(1, 1);

        DatabaseServerV4Response dbResponse = new DatabaseServerV4Response();
        dbResponse.setCrn(DB_CRN);
        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(Set.of(dbResponse));

        AttemptMaker<List<FlowIdentifier>> attemptMaker = () -> {
            throw new PollerStoppedException("Timed out");
        };

        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS)).thenReturn(attemptMaker);

        StackOperationFailedException exception = assertThrows(StackOperationFailedException.class,
                () -> underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS, null));

        assertEquals("Starting database user defined tags update timed out", exception.getMessage());
    }

    @Test
    void testUpdateUserDefinedTagsWhenPollerAborted() {
        setPollingConfig(5, 1);

        DatabaseServerV4Response dbResponse = new DatabaseServerV4Response();
        dbResponse.setCrn(DB_CRN);
        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(Set.of(dbResponse));

        AttemptMaker<List<FlowIdentifier>> attemptMaker = () -> {
            throw new UserBreakException("Aborted");
        };

        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS)).thenReturn(attemptMaker);

        StackOperationFailedException exception = assertThrows(StackOperationFailedException.class,
                () -> underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS, null));

        assertEquals("Starting database user defined tags update timed out", exception.getMessage());
    }

    @Test
    void testDeleteUserDefinedTags() {
        setPollingConfig(5, 1);

        DatabaseServerV4Response dbResponse = new DatabaseServerV4Response();
        dbResponse.setCrn(DB_CRN);
        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(Set.of(dbResponse));

        FlowIdentifier expectedFlow = new FlowIdentifier(FlowType.FLOW, "flow-id-1");
        AttemptMaker<List<FlowIdentifier>> triggerAttemptMaker = () -> AttemptResults.finishWith(List.of(expectedFlow));
        AttemptMaker<Void> completionAttemptMaker = () -> AttemptResults.finishWith(null);

        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsDeletePoller(List.of(DB_CRN), ENV_ID, TAG_KEYS)).thenReturn(triggerAttemptMaker);
        when(redbeamsPollerProvider.userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID)).thenReturn(completionAttemptMaker);

        underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, null, TAG_KEYS);

        verify(databaseServerV4Endpoint).list(ENV_CRN);
        verify(redbeamsPollerProvider).userDefinedTagsDeletePoller(List.of(DB_CRN), ENV_ID, TAG_KEYS);
        verify(redbeamsPollerProvider).userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID);
    }

    @Test
    void testDeleteUserDefinedTagsSkipsStoppedFailedAndDeletingDatabases() {
        setPollingConfig(5, 1);

        DatabaseServerV4Response availableDb = databaseServer(DB_CRN, Status.AVAILABLE);
        DatabaseServerV4Response updateFailedDb = databaseServer("updateFailedDbCrn", Status.UPDATE_FAILED);
        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(new LinkedHashSet<>(List.of(
                availableDb,
                updateFailedDb,
                databaseServer("stoppedDbCrn", Status.STOPPED),
                databaseServer("createFailedDbCrn", Status.CREATE_FAILED),
                databaseServer("deletingDbCrn", Status.DELETE_IN_PROGRESS),
                databaseServer("deletedOnProviderDbCrn", Status.DELETED_ON_PROVIDER_SIDE))));

        FlowIdentifier expectedFlow = new FlowIdentifier(FlowType.FLOW, "flow-id-1");
        List<String> expectedDbCrns = List.of(DB_CRN, "updateFailedDbCrn");
        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsDeletePoller(expectedDbCrns, ENV_ID, TAG_KEYS))
                .thenReturn(() -> AttemptResults.finishWith(List.of(expectedFlow)));
        when(redbeamsPollerProvider.userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID))
                .thenReturn(() -> AttemptResults.finishWith(null));

        underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, null, TAG_KEYS);

        verify(redbeamsPollerProvider).userDefinedTagsDeletePoller(expectedDbCrns, ENV_ID, TAG_KEYS);
    }

    @Test
    void testDeleteUserDefinedTagsFailsWithDeleteSpecificMessage() {
        setPollingConfig(5, 1);

        DatabaseServerV4Responses responses = new DatabaseServerV4Responses(Set.of(databaseServer(DB_CRN, Status.AVAILABLE)));
        FlowIdentifier expectedFlow = new FlowIdentifier(FlowType.FLOW, "flow-id-1");
        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsDeletePoller(List.of(DB_CRN), ENV_ID, TAG_KEYS))
                .thenReturn(() -> AttemptResults.finishWith(List.of(expectedFlow)));
        when(redbeamsPollerProvider.userDefinedTagsFlowsCompletionPoller(List.of(expectedFlow), ENV_ID))
                .thenReturn(() -> AttemptResults.breakFor("Modify user defined tags on DB stack failed."));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.modifyUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, null, TAG_KEYS));

        assertThat(exception.getMessage()).startsWith("Database user defined tags deletion");
    }

    private DatabaseServerV4Response databaseServer(String crn, Status status) {
        DatabaseServerV4Response databaseServer = new DatabaseServerV4Response();
        databaseServer.setCrn(crn);
        databaseServer.setStatus(status);
        return databaseServer;
    }

    private void setPollingConfig(int maxTime, int sleepTime) {
        ReflectionTestUtils.setField(underTest, "maxTime", maxTime);
        ReflectionTestUtils.setField(underTest, "sleepTime", sleepTime);
    }
}