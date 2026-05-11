package com.sequenceiq.environment.environment.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.environment.exception.StackOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Responses;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerServiceTest {

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "envCrn";

    private static final String DB_CRN = "dbCrn";

    private static final Map<String, String> TAGS = Map.of("custom", "value");

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
        AttemptMaker<List<FlowIdentifier>> attemptMaker = () -> com.dyngr.core.AttemptResults.finishWith(List.of(expectedFlow));

        when(databaseServerV4Endpoint.list(ENV_CRN)).thenReturn(responses);
        when(redbeamsPollerProvider.userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS)).thenReturn(attemptMaker);

        List<FlowIdentifier> result = underTest.updateUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS);

        assertEquals(List.of(expectedFlow), result);
        verify(databaseServerV4Endpoint).list(ENV_CRN);
        verify(redbeamsPollerProvider).userDefinedTagsUpdatePoller(List.of(DB_CRN), ENV_ID, TAGS);
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
                () -> underTest.updateUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS));

        assertEquals("DB stack user defined tags updating timed out", exception.getMessage());
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
                () -> underTest.updateUserDefinedTagsOnDatabases(ENV_ID, ENV_CRN, TAGS));

        assertEquals("DB stack user defined tags updating timed out", exception.getMessage());
    }

    private void setPollingConfig(int maxTime, int sleepTime) {
        ReflectionTestUtils.setField(underTest, "maxTime", maxTime);
        ReflectionTestUtils.setField(underTest, "sleepTime", sleepTime);
    }
}