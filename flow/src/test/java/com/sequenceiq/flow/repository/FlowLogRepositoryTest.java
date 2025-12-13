package com.sequenceiq.flow.repository;

import static com.sequenceiq.flow.api.model.operation.OperationType.PROVISION;
import static com.sequenceiq.flow.api.model.operation.OperationType.UNKNOWN;
import static com.sequenceiq.flow.domain.StateStatus.FAILED;
import static com.sequenceiq.flow.domain.StateStatus.PENDING;
import static com.sequenceiq.flow.domain.StateStatus.SUCCESSFUL;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.repository.RepositoryTestConfig.RepositoryTestInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = RepositoryTestInitializer.class, classes = RepositoryTestConfig.class)
@DataJpaTest
class FlowLogRepositoryTest {

    private static final AtomicInteger TEST_RESOURCE_ID = new AtomicInteger(1234);

    private static final String TEST_CURRENT_FLOW_STATE = "CURRENT_FLOW_STATE";

    @Inject
    private FlowLogRepository underTest;

    @Test
    void testPurgeFinalizedSuccessfulFlowLogs() {
        FlowLog flowLog1 = createFlowLog(Boolean.TRUE, PENDING, PROVISION);

        FlowLog flowLog2 = createFlowLog(Boolean.TRUE, PENDING, PROVISION);
        flowLog2.setEndTime(nowMinus(Duration.of(45, MINUTES)).toEpochMilli());

        FlowLog flowLog3 = createFlowLog(Boolean.TRUE, PENDING, PROVISION);
        flowLog3.setEndTime(nowMinus(Duration.of(30, MINUTES)).toEpochMilli());

        FlowLog flowLog4 = createFlowLog(Boolean.TRUE, SUCCESSFUL, UNKNOWN);
        flowLog4.setEndTime(nowMinus(Duration.of(20, MINUTES)).toEpochMilli());

        FlowLog flowLog5 = createFlowLog(Boolean.FALSE, SUCCESSFUL, PROVISION);
        FlowLog flowLog6 = createFlowLog(Boolean.FALSE, FAILED, UNKNOWN);

        saveFlowLogs(flowLog1, flowLog2, flowLog3, flowLog4, flowLog5, flowLog6);

        int result = underTest.purgeFinalizedSuccessfulFlowLogs(nowMinus(Duration.of(30, MINUTES)).toEpochMilli());

        assertThat(result).isEqualTo(2);
    }

    @Test
    void testFindAllByFlowIdsCreatedDesc() {
        String flowId1 = randomUUID().toString();
        FlowLog flowLog1 = createFlowLog(flowId1);
        flowLog1.setCreated(1L);
        String flowId2 = randomUUID().toString();
        FlowLog flowLog2 = createFlowLog(flowId2);
        flowLog2.setCreated(2L);
        String flowId3 = randomUUID().toString();
        FlowLog flowLog3 = createFlowLog(flowId3);
        flowLog3.setCreated(3L);
        String flowId4 = randomUUID().toString();
        FlowLog flowLog4 = createFlowLog(flowId4);
        flowLog4.setCreated(4L);

        saveFlowLogs(flowLog1, flowLog2, flowLog3, flowLog4);

        Page<FlowLog> result = underTest.findAllByFlowIdsCreatedDesc(Set.of(flowId1, flowId2, flowId3, flowId4),
                PageRequest.of(0, 4));

        assertThat(result.getContent().size()).isEqualTo(4);
        assertTrue(result.getContent().contains(flowLog1));
        assertTrue(result.getContent().contains(flowLog2));
        assertTrue(result.getContent().contains(flowLog3));
        assertTrue(result.getContent().contains(flowLog4));
    }

    @Test
    void testFindAllByFlowIdsCreatedDescPaginated() {
        String flowId1 = randomUUID().toString();
        FlowLog flowLog1 = createFlowLog(flowId1);
        flowLog1.setCreated(1L);
        String flowId2 = randomUUID().toString();
        FlowLog flowLog2 = createFlowLog(flowId2);
        flowLog2.setCreated(2L);
        String flowId3 = randomUUID().toString();
        FlowLog flowLog3 = createFlowLog(flowId3);
        flowLog3.setCreated(3L);
        String flowId4 = randomUUID().toString();
        FlowLog flowLog4 = createFlowLog(flowId4);
        flowLog4.setCreated(4L);

        saveFlowLogs(flowLog1, flowLog2, flowLog3, flowLog4);

        Page<FlowLog> resultPaginated = underTest.findAllByFlowIdsCreatedDesc(Set.of(flowId1, flowId2, flowId3, flowId4),
                PageRequest.of(0, 2));

        assertThat(resultPaginated.getContent().size()).isEqualTo(2);
        assertTrue(resultPaginated.getContent().contains(flowLog3));
        assertTrue(resultPaginated.getContent().contains(flowLog4));
        assertThat(resultPaginated.getTotalElements()).isEqualTo(4);

        assertTrue(resultPaginated.hasNext());

        resultPaginated = underTest.findAllByFlowIdsCreatedDesc(Set.of(flowId1, flowId2, flowId3, flowId4),
                resultPaginated.nextPageable());

        assertThat(resultPaginated.getContent().size()).isEqualTo(2);
        assertTrue(resultPaginated.getContent().contains(flowLog1));
        assertTrue(resultPaginated.getContent().contains(flowLog2));
        assertThat(resultPaginated.getTotalElements()).isEqualTo(4);
    }

    private Instant nowMinus(Duration of) {
        return now().minus(of);
    }

    private FlowLog createFlowLog(boolean finalized, StateStatus status, OperationType type) {
        return new FlowLog((long) TEST_RESOURCE_ID.incrementAndGet(), randomUUID().toString(), TEST_CURRENT_FLOW_STATE, finalized, status, type);
    }

    private FlowLog createFlowLog(String flowId) {
        return new FlowLog((long) TEST_RESOURCE_ID.incrementAndGet(), flowId,
                randomUUID().toString(), "TEST_USR_CRN", "TEST_NEXT_FLOW_STATE", null,
                null, null, null, TEST_CURRENT_FLOW_STATE);
    }

    private void saveFlowLogs(FlowLog... flowLogs) {
        underTest.saveAll(asList(flowLogs));
    }
}
