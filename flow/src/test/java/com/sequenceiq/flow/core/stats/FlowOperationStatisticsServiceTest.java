package com.sequenceiq.flow.core.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.core.cache.FlowStat;
import com.sequenceiq.flow.repository.FlowOperationStatsRepository;

@ExtendWith(MockitoExtension.class)
public class FlowOperationStatisticsServiceTest {

    private static final String SAMPLE_RESOURCE_CRN = "crn:cdp:environments:us-west-1:12345-6789:environment:12345-6789";

    private static final String SAMPLE_CLOUD_PLATFORM = "AWS";

    private static final Date OLD_DATE = new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime();

    private static final Double EXPECTED_TWO_MINUTES = 120d;

    private static final Integer EXPECTED_PROGRESS_PERCENT = 99;

    @InjectMocks
    private FlowOperationStatisticsService underTest;

    @Mock
    private FlowOperationStatsRepository flowOperationStatsRepository;

    @Mock
    private TransactionService transactionService;

    @BeforeEach
    public void setUp() {
        underTest = new FlowOperationStatisticsService(flowOperationStatsRepository, transactionService);
    }

    @Test
    public void testUpdateOperationAverageTime() {
        // GIVEN
        // WHEN
        underTest.updateOperationAverageTime(OperationType.PROVISION, SAMPLE_CLOUD_PLATFORM, "120");
        Double result = underTest.getExpectedAverageTimeForOperation(OperationType.PROVISION, SAMPLE_CLOUD_PLATFORM);
        // THEN
        assertEquals(EXPECTED_TWO_MINUTES, result);
    }

    @Test
    public void testGetProgressFromHistory() {
        // GIVEN
        // WHEN
        underTest.updateOperationAverageTime(OperationType.PROVISION, SAMPLE_CLOUD_PLATFORM, "120");
        Integer result = underTest.getProgressFromHistory(createFlowStat());
        // THEN
        assertEquals(EXPECTED_PROGRESS_PERCENT, result);
    }

    private FlowStat createFlowStat() {
        FlowStat flowStat = new FlowStat();
        PayloadContext payloadContext = PayloadContext.create(SAMPLE_RESOURCE_CRN, SAMPLE_CLOUD_PLATFORM);
        flowStat.setPayloadContext(payloadContext);
        flowStat.setOperationType(OperationType.PROVISION);
        flowStat.setStartTime(OLD_DATE.getTime());
        return flowStat;
    }
}
