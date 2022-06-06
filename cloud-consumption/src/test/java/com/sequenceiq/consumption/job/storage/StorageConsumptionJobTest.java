package com.sequenceiq.consumption.job.storage;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.flow.ConsumptionReactorFlowManager;
import com.sequenceiq.consumption.service.ConsumptionService;

@ExtendWith(MockitoExtension.class)
public class StorageConsumptionJobTest {

    private static final Long LOCAL_ID = 42L;

    private static final String REMOTE_CRN = "remote-crn";

    private static final Long CONSUMPTION_ID = 1L;

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private ConsumptionReactorFlowManager flowManager;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private Consumption consumption;

    @Mock
    private RegionAwareInternalCrnGenerator crnGenerator;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private StorageConsumptionJob underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest.setLocalId(LOCAL_ID.toString());
        underTest.setRemoteResourceCrn(REMOTE_CRN);
        consumption.setId(CONSUMPTION_ID);
    }

    @Test
    public void testGetMdcContextObjectThrowsError() {
        when(consumptionService.findConsumptionById(LOCAL_ID)).thenThrow(new NotFoundException("error"));

        Assertions.assertThrows(NotFoundException.class, () -> underTest.getMdcContextObject());
    }

    @Test
    public void testGetMdcContextObject() {
        when(consumptionService.findConsumptionById(LOCAL_ID)).thenReturn(consumption);

        Assertions.assertEquals(consumption, underTest.getMdcContextObject());
    }

    @Test
    public void testExecuteTracedJob() {
        when(consumptionService.findConsumptionById(LOCAL_ID)).thenReturn(consumption);
        when(regionAwareInternalCrnGeneratorFactory.consumption()).thenReturn(crnGenerator);
        String crn = "internal-crn";
        when(crnGenerator.getInternalCrnForServiceAsString()).thenReturn(crn);
        doNothing().when(flowManager).triggerStorageConsumptionCollectionFlow(consumption, crn);

        Assertions.assertDoesNotThrow(() -> underTest.executeTracedJob(jobExecutionContext));
        verify(consumptionService).findConsumptionById(LOCAL_ID);
        verify(regionAwareInternalCrnGeneratorFactory).consumption();
        verify(crnGenerator).getInternalCrnForServiceAsString();
        verify(flowManager).triggerStorageConsumptionCollectionFlow(consumption, crn);
    }

    @Test
    public void testExecuteTracedJobThrowsError() {
        when(consumptionService.findConsumptionById(LOCAL_ID)).thenThrow(new NotFoundException("error"));

        Assertions.assertThrows(JobExecutionException.class, () -> underTest.executeTracedJob(jobExecutionContext));
        verify(consumptionService).findConsumptionById(LOCAL_ID);
    }
}
