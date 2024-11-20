package com.sequenceiq.cloudbreak.rotation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hive.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.service.rdsconfig.HiveRdsConfigProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(MockitoExtension.class)
public class SharedDBRotationUtilsTest {

    @Mock
    private RedbeamsDbServerConfigurer redbeamsDbServerConfigurer;

    @Mock
    private DatabaseCommon dbCommon;

    @Mock
    private HiveRdsConfigProvider hiveRdsConfigProvider;

    @Mock
    private HiveMetastoreConfigProvider hiveMetastoreConfigProvider;

    @Mock
    private FlowService flowService;

    @InjectMocks
    private SharedDBRotationUtils underTest;

    @Test
    void testFlowPollingIfFailed() {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(Boolean.FALSE);
        flowCheckResponse.setLatestFlowFinalizedAndFailed(Boolean.TRUE);
        when(flowService.getFlowState(any())).thenReturn(flowCheckResponse);

        assertThrows(UserBreakException.class, () -> underTest.pollFlow(new FlowIdentifier(FlowType.FLOW, "1")));
    }

    @Test
    void testFlowPolling() {
        FlowCheckResponse flowCheckResponse = new FlowCheckResponse();
        flowCheckResponse.setHasActiveFlow(Boolean.TRUE);
        FlowCheckResponse flowCheckResponse2 = new FlowCheckResponse();
        flowCheckResponse2.setHasActiveFlow(Boolean.FALSE);
        flowCheckResponse2.setLatestFlowFinalizedAndFailed(Boolean.FALSE);
        when(flowService.getFlowState(any())).thenReturn(flowCheckResponse).thenReturn(flowCheckResponse2);

        underTest.pollFlow(new FlowIdentifier(FlowType.FLOW, "1"));

        verify(flowService, times(2)).getFlowState(any());
    }
}
