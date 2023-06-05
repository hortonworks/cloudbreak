package com.sequenceiq.flow.repository;

import static java.util.Arrays.asList;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.repository.RepositoryTestConfig.RepositoryTestInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = RepositoryTestInitializer.class, classes = RepositoryTestConfig.class)
@DataJpaTest
class FlowChainLogRepositoryTest {
    @Inject
    private FlowChainLogRepository underTest;

    @Test
    void testFindByFlowChainIdOrderByCreatedDescIdDescWithSameCreated() {
        FlowChainLog flowChainLog1 = new FlowChainLog();
        flowChainLog1.setFlowChainId("FLOW_CHAIN_ID");
        flowChainLog1.setCreated(888L);
        flowChainLog1.setChainJackson("FIRST_ENTITY_PUT_IN");
        FlowChainLog flowChainLog2 = new FlowChainLog();
        flowChainLog2.setFlowChainId("FLOW_CHAIN_ID");
        flowChainLog2.setCreated(888L);
        flowChainLog2.setChainJackson("SECOND_ENTITY_PUT_IN");

        saveFlowChainLogs(flowChainLog1, flowChainLog2);

        List<FlowChainLog> result = underTest.findByFlowChainIdOrderByCreatedDescIdDesc("FLOW_CHAIN_ID");
        Assertions.assertEquals("SECOND_ENTITY_PUT_IN", result.get(0).getChainJackson());
    }

    @Test
    void testFindByFlowChainIdOrderByCreatedDescIdDescWithDifferentCreated() {
        FlowChainLog flowChainLog1 = new FlowChainLog();
        flowChainLog1.setFlowChainId("FLOW_CHAIN_ID");
        flowChainLog1.setCreated(889L);
        flowChainLog1.setChainJackson("FIRST_ENTITY_PUT_IN");
        FlowChainLog flowChainLog2 = new FlowChainLog();
        flowChainLog2.setFlowChainId("FLOW_CHAIN_ID");
        flowChainLog2.setCreated(888L);
        flowChainLog2.setChainJackson("SECOND_ENTITY_PUT_IN");

        saveFlowChainLogs(flowChainLog1, flowChainLog2);

        List<FlowChainLog> result = underTest.findByFlowChainIdOrderByCreatedDescIdDesc("FLOW_CHAIN_ID");
        Assertions.assertEquals("FIRST_ENTITY_PUT_IN", result.get(0).getChainJackson());

    }

    private void saveFlowChainLogs(FlowChainLog... flowChainLogs) {
        underTest.saveAll(asList(flowChainLogs));
    }
}