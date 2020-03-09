package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

class DatabaseCriteriaResolverTest {

    private final DatabaseCriteriaResolver underTest = new DatabaseCriteriaResolver();

    private Cluster cluster;

    private DatabaseServerV4Response database;

    @BeforeEach
    void setUp() {
        database = new DatabaseServerV4Response();
        cluster = new Cluster();
    }

    @Test
    void nullDatabaseOperationThrows() {
        assertThatThrownBy(() -> underTest.resolveResultByCriteria(null, database, cluster));
    }

    @Test
    void nullDatabaseThrows() {
        assertThatThrownBy(() -> underTest.resolveResultByCriteria(DatabaseOperation.CREATION, null, cluster));
    }

    @Test
    void exitCriteria() {
        database.setStatus(Status.AVAILABLE);
        AttemptResult<Object> result = underTest.resolveResultByCriteria(DatabaseOperation.CREATION, database, cluster);
        assertThat(result.getState()).isEqualTo(AttemptState.FINISH);
        assertThat(result.getResult()).isEqualTo(database);
    }

    @Test
    void failCriteriaDatabaseNotFound() {
        database.setStatus(Status.DELETE_COMPLETED);
        database.setStatusReason("does not exist");
        AttemptResult<Object> result = underTest.resolveResultByCriteria(DatabaseOperation.CREATION, database, cluster);
        assertThat(result.getState()).isEqualTo(AttemptState.FINISH);
        assertThat(result.getResult()).isNull();
    }

    @Test
    void failCriteriaDatabaseFail() {
        database.setStatus(Status.CREATE_FAILED);
        AttemptResult<Object> result = underTest.resolveResultByCriteria(DatabaseOperation.CREATION, database, cluster);
        assertThat(result.getState()).isEqualTo(AttemptState.BREAK);
        assertThat(result.getResult()).isNull();
    }

    @Test
    void neitherExitNorFailure() {
        database.setStatus(Status.CREATE_IN_PROGRESS);
        AttemptResult<Object> result = underTest.resolveResultByCriteria(DatabaseOperation.CREATION, database, cluster);
        assertThat(result.getState()).isEqualTo(AttemptState.CONTINUE);
        assertThat(result.getResult()).isNull();
    }
}
