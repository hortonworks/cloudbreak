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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = RepositoryTestConfig.RepositoryTestInitializer.class, classes = RepositoryTestConfig.class)
@DataJpaTest
class FlowLogRepositoryTest {

    private static final AtomicInteger TEST_RESOURCE_ID = new AtomicInteger(1234);

    private static final String TEST_CURRENT_FLOW_STATE = "CURRENT_FLOW_STATE";

    @Inject
    private FlowLogRepository underTest;

    @Test
    void testPurgeFinalizedFlowLogs() {
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

        int result = underTest.purgeFinalizedFlowLogs(nowMinus(Duration.of(30, MINUTES)).toEpochMilli());

        assertThat(result).isEqualTo(2);
    }

    private Instant nowMinus(Duration of) {
        return now().minus(of);
    }

    private FlowLog createFlowLog(boolean finalized, StateStatus status, OperationType type) {
        return new FlowLog((long) TEST_RESOURCE_ID.incrementAndGet(), randomUUID().toString(), TEST_CURRENT_FLOW_STATE, finalized, status, type);
    }

    private void saveFlowLogs(FlowLog... flowLogs) {
        underTest.saveAll(asList(flowLogs));
    }
}