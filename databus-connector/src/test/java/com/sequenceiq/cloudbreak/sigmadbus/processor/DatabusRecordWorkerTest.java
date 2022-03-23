package com.sequenceiq.cloudbreak.sigmadbus.processor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.BlockingDeque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.SigmaDatabusClient;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRecordProcessingException;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class DatabusRecordWorkerTest {

    private DatabusRecordWorker<MonitoringConfiguration> underTest;

    @Mock
    private Tracer tracer;

    @Mock
    private BlockingDeque<DatabusRequest> processingQueue;

    @Mock
    private AbstractDatabusRecordProcessor<MonitoringConfiguration> databusRecordProcessor;

    @Mock
    private SigmaDatabusClient<MonitoringConfiguration> dataBusClient;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @BeforeEach
    public void setUp() {
        underTest = new DatabusRecordWorker<>("thread-name", processingQueue, databusRecordProcessor, tracer,
                regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void testProcessRecordInput() throws DatabusRecordProcessingException {
        // GIVEN
        underTest = spy(underTest);
        doReturn(dataBusClient).when(underTest).getClient();
        doNothing().when(dataBusClient).putRecord(any(DatabusRequest.class));
        // WHEN
        underTest.processRecordInput(createDatabusRequest());
        // THEN
        verify(dataBusClient, times(1)).putRecord(any(DatabusRequest.class));
    }

    @Test
    public void testProcessRecordInputThrowingDatabusRecordException() throws DatabusRecordProcessingException {
        // GIVEN
        underTest = spy(underTest);
        doReturn(dataBusClient).when(underTest).getClient();
        doThrow(new DatabusRecordProcessingException("error")).when(dataBusClient).putRecord(any(DatabusRequest.class));
        doNothing().when(databusRecordProcessor).handleDatabusRecordProcessingException(
                any(DatabusRequest.class), any(DatabusRecordProcessingException.class));
        // WHEN
        underTest.processRecordInput(createDatabusRequest());
        // THEN
        verify(databusRecordProcessor, times(1)).handleDatabusRecordProcessingException(
                any(DatabusRequest.class), any(DatabusRecordProcessingException.class));
    }

    @Test
    public void testProcessRecordInputThrowingUnexpectedException() throws DatabusRecordProcessingException {
        // GIVEN
        underTest = spy(underTest);
        doReturn(dataBusClient).when(underTest).getClient();
        doThrow(new NullPointerException()).when(dataBusClient).putRecord(any(DatabusRequest.class));
        doNothing().when(databusRecordProcessor).handleUnexpectedException(any(DatabusRequest.class), any(Exception.class));
        // WHEN
        underTest.processRecordInput(createDatabusRequest());
        // THEN
        verify(databusRecordProcessor, times(1)).handleUnexpectedException(any(DatabusRequest.class), any(Exception.class));
    }

    @Test
    public void testProcessGetClient() {
        // GIVEN
        // WHEN
        SigmaDatabusClient<MonitoringConfiguration> clientResult = underTest.getClient();
        // THEN
        assertNotNull(clientResult);
    }

    private DatabusRequest createDatabusRequest() {
        return DatabusRequest.Builder.newBuilder()
                .withRawBody("{}")
                .withContext(DatabusRequestContext.Builder.newBuilder()
                        .withAccountId("cloudera")
                        .build())
                .build();
    }
}
