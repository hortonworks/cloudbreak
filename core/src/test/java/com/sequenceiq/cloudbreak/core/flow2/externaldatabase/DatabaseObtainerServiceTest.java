package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterPollingCheckerService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class DatabaseObtainerServiceTest {

    @Mock
    private RedbeamsClientService redbeamsClient;

    @Mock
    private ClusterPollingCheckerService clusterPollingCheckerService;

    @Mock
    private DatabaseCriteriaResolver databaseCriteriaResolver;

    @InjectMocks
    private DatabaseObtainerService underTest;

    private Cluster cluster;

    @BeforeEach
    void setUp() {
        cluster = new Cluster();
    }

    @Test
    void cancelledStateIsNull() throws JsonProcessingException {
        AttemptResult<Object> attemptResult = AttemptResults.justContinue();
        when(clusterPollingCheckerService.checkClusterCancelledState(any(), anyBoolean())).thenReturn(attemptResult);
        AttemptResult<Object> result = underTest.obtainAttemptResult(cluster, DatabaseOperation.CREATION, "crn", true);
        assertThat(result.getState()).isEqualTo(attemptResult.getState());
    }

    @Test
    void rdsNotFound() throws JsonProcessingException {
        when(clusterPollingCheckerService.checkClusterCancelledState(any(), anyBoolean())).thenReturn(null);
        when(redbeamsClient.getByCrn(anyString())).thenThrow(NotFoundException.class);
        AttemptResult<Object> result = underTest.obtainAttemptResult(cluster, DatabaseOperation.CREATION, "crn", true);
        assertThat(result.getState()).isEqualTo(AttemptState.FINISH);
        assertThat(result.getResult()).isNull();
    }

    @Test
    void attemptResultObtained() throws JsonProcessingException {
        when(clusterPollingCheckerService.checkClusterCancelledState(any(), anyBoolean())).thenReturn(null);
        when(redbeamsClient.getByCrn(anyString())).thenReturn(new DatabaseServerV4Response());
        AttemptResult<Object> attemptResult = AttemptResults.justContinue();
        when(databaseCriteriaResolver.resolveResultByCriteria(any(), any(), any())).thenReturn(attemptResult);
        AttemptResult<Object> result = underTest.obtainAttemptResult(cluster, DatabaseOperation.CREATION, "crn", true);
        assertThat(result.getState()).isEqualTo(attemptResult.getState());
        assertThat(result.getResult()).isNull();
    }
}
