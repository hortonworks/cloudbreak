package com.sequenceiq.flow.core.stats;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.operation.OperationFlowsView;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.converter.FlowProgressResponseConverter;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.FlowOperationStats;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;

@ExtendWith(MockitoExtension.class)
public class FlowOperationStatisticsServiceTest {

    private static final String DUMMY_CRN = "crn:cdp:environments:us-west-1:1234:environment:myenv";

    private static final Integer DUMMY_PROGRESS = 66;

    private FlowOperationStatisticsService underTest;

    @Mock
    private FlowOperationStatsRepository flowOperationStatsRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private PayloadContextProvider payloadContextProvider;

    @Mock
    private FlowProgressResponseConverter flowProgressResponseConverter;

    @Mock
    private AbstractFlowConfiguration flowConfiguration;

    @BeforeEach
    public void setUp() {
        underTest = new FlowOperationStatisticsService(flowOperationStatsRepository, payloadContextProvider,
                transactionService, flowProgressResponseConverter);
    }

    @Test
    public void testCreateOperationResponse() {
        // GIVEN
        given(payloadContextProvider.getPayloadContext(1L)).willReturn(getPayloadContext());
        given(flowOperationStatsRepository.findFirstByOperationTypeAndCloudPlatform(any(OperationType.class), anyString())).willReturn(createFlowStats());
        given(flowProgressResponseConverter.convert(any(List.class), anyString())).willReturn(createFlowProgressResponse());
        // WHEN
        Optional<OperationFlowsView> response = underTest.createOperationResponse(DUMMY_CRN, flowLogs());
        // THEN
        assertFalse(response.get().getFlowTypeProgressMap().isEmpty());
        verify(flowOperationStatsRepository, times(1)).findFirstByOperationTypeAndCloudPlatform(any(OperationType.class), anyString());
    }

    @Test
    public void testCreateOperationResponseWithoutFlowLogs() {
        // GIVEN
        // WHEN
        Optional<OperationFlowsView> response = underTest.createOperationResponse(DUMMY_CRN, new ArrayList<>());
        // THEN
        assertTrue(response.isEmpty());
    }

    private List<FlowLog> flowLogs() {
        List<FlowLog> result = new ArrayList<>();
        FlowLog fl1 = new FlowLog();
        fl1.setOperationType(OperationType.PROVISION);
        fl1.setFlowId("flowid");
        fl1.setResourceId(1L);
        fl1.setFlowType(ClassValue.of(flowConfiguration.getClass()));
        result.add(fl1);
        return result;
    }

    private PayloadContext getPayloadContext() {
        return PayloadContext.create(DUMMY_CRN, CloudPlatform.AWS.name());
    }

    private FlowProgressResponse createFlowProgressResponse() {
        FlowProgressResponse response = new FlowProgressResponse();
        response.setProgress(DUMMY_PROGRESS);
        response.setFinalized(false);
        response.setFlowId("flowid");
        response.setCreated(new Date().getTime());
        return response;
    }

    private Optional<FlowOperationStats> createFlowStats() {
        FlowOperationStats flowOperationStats = new FlowOperationStats();
        flowOperationStats.setCloudPlatform(CloudPlatform.AWS.name());
        flowOperationStats.setOperationType(OperationType.PROVISION);
        flowOperationStats.setDurationHistory("66,69");
        return Optional.of(flowOperationStats);
    }

}
