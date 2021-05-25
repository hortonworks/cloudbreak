package com.sequenceiq.cdp.databus.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.doNothing;

import java.util.concurrent.BlockingDeque;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cdp.databus.cache.AccountDatabusConfigCache;
import com.sequenceiq.cdp.databus.client.DatabusClient;
import com.sequenceiq.cdp.databus.model.DatabusRecordInput;
import com.sequenceiq.cdp.databus.model.DatabusRequestContext;
import com.sequenceiq.cdp.databus.model.PutRecordResponse;
import com.sequenceiq.cdp.databus.model.exception.DatabusRecordProcessingException;
import com.sequenceiq.cdp.databus.service.AccountDatabusConfigService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@ExtendWith(MockitoExtension.class)
public class DatabusRecordWorkerTest {

    private DatabusRecordWorker<MetricsDatabusRecordProcessor> underTest;

    @Mock
    private MetricsDatabusRecordProcessor databusRecordProcessor;

    @Mock
    private BlockingDeque<DatabusRecordInput> processingQueue;

    @Mock
    private DatabusClient.Builder clientBuilder;

    @Mock
    private AccountDatabusConfigService accountDatabusConfigService;

    @Mock
    private AccountDatabusConfigCache accountDatabusConfigCache;

    @Mock
    private MonitoringConfiguration monitoringConfiguration;

    @Mock
    private DataBusCredential dataBusCredential;

    @Mock
    private DatabusClient databusClient;

    @Mock
    private PutRecordResponse response;

    @BeforeEach
    public void setUp() {
        underTest = new DatabusRecordWorker<>("thread-name", clientBuilder, accountDatabusConfigService,
                accountDatabusConfigCache, processingQueue, databusRecordProcessor);
    }

    @Test
    public void testProcessRecordInput() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(response.getHttpCode()).willReturn(200);
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(1)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(1)).putRecord(any(), any());
        verify(response, times(1)).getHttpCode();
        verify(databusRecordProcessor, times(0)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
        verify(databusRecordProcessor, times(0)).handleUnexpectedException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWithRedirect() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(response.getHttpCode()).willReturn(302);
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(1)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(1)).putRecord(any(), any());
        verify(response, times(1)).getHttpCode();
        verify(databusRecordProcessor, times(0)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
        verify(databusRecordProcessor, times(0)).handleUnexpectedException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWithNonAuthClientError()
            throws DatabusRecordProcessingException, TransactionService.TransactionExecutionException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(response.getHttpCode()).willReturn(410);
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(1)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(1)).putRecord(any(), any());
        verify(response, times(1)).getHttpCode();
        verify(databusRecordProcessor, times(1)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
        verify(accountDatabusConfigService, times(0)).checkMachineUserWithAccessKeyStillExists(anyString(), any());
    }

    @Test
    public void testProcessRecordInputWith403ClientErrorWithSuccessfulRetry()
            throws DatabusRecordProcessingException, TransactionService.TransactionExecutionException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(accountDatabusConfigService.checkMachineUserWithAccessKeyStillExists(anyString(), any())).willReturn(false);
        doNothing().when(accountDatabusConfigService).cleanupCacheAndDbForAccountIdAndName(anyString(), anyString(), any());
        given(response.getHttpCode()).willReturn(403).willReturn(200);
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(2)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(2)).putRecord(any(), any());
        verify(response, times(2)).getHttpCode();
        verify(accountDatabusConfigService, times(1)).checkMachineUserWithAccessKeyStillExists(anyString(), any());
        verify(accountDatabusConfigService, times(1)).cleanupCacheAndDbForAccountIdAndName(anyString(), anyString(), any());
        verify(databusRecordProcessor, times(0)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWith403ClientErrorWithFailedRetry() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(accountDatabusConfigService.checkMachineUserWithAccessKeyStillExists(anyString(), any())).willReturn(true);
        given(response.getHttpCode()).willReturn(403).willReturn(403);
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(2)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(2)).putRecord(any(), any());
        verify(response, times(2)).getHttpCode();
        verify(accountDatabusConfigService, times(1)).checkMachineUserWithAccessKeyStillExists(anyString(), any());
        verify(databusRecordProcessor, times(1)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWithServerError() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(response.getHttpCode()).willReturn(504);
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(1)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(1)).putRecord(any(), any());
        verify(response, times(1)).getHttpCode();
        verify(accountDatabusConfigService, times(0)).checkMachineUserWithAccessKeyStillExists(anyString(), any());
        verify(databusRecordProcessor, times(1)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWithUnexpectedError() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willReturn(dataBusCredential);
        given(clientBuilder.build()).willReturn(databusClient);
        given(databusClient.putRecord(any(), any())).willReturn(response);
        given(response.getHttpCode()).willThrow(new NullPointerException());
        doNothing().when(databusRecordProcessor).handleUnexpectedException(any(), anyString(), anyString(), any());
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).getMachineUserName(anyString());
        verify(accountDatabusConfigService, times(1)).getOrCreateDataBusCredentials(anyString(), anyString(), any());
        verify(clientBuilder, times(1)).build();
        verify(databusClient, times(1)).putRecord(any(), any());
        verify(response, times(1)).getHttpCode();
        verify(databusRecordProcessor, times(1)).handleUnexpectedException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWithNull() {
        // GIVEN
        // WHEN
        underTest.processRecordInput(null);
        // THEN
        verify(databusRecordProcessor, times(0)).getMachineUserName(anyString());
    }

    @Test
    public void testProcessRecordInputWithFailingGetDatabusCredential() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any()))
                .willThrow(new DatabusRecordProcessingException("exception"));
        doNothing().when(databusRecordProcessor).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).handleDatabusRecordProcessingException(any(), anyString(), anyString(), any());
    }

    @Test
    public void testProcessRecordInputWithUnexpectedException() throws DatabusRecordProcessingException {
        // GIVEN
        given(databusRecordProcessor.getMachineUserName(anyString())).willReturn("machine-user");
        given(accountDatabusConfigService.getOrCreateDataBusCredentials(anyString(), anyString(), any())).willThrow(new NullPointerException());
        doNothing().when(databusRecordProcessor).handleUnexpectedException(any(), anyString(), anyString(), any());
        // WHEN
        underTest.processRecordInput(createInput());
        // THEN
        verify(databusRecordProcessor, times(1)).handleUnexpectedException(any(), anyString(), anyString(), any());
    }

    private DatabusRecordInput createInput() {
        return DatabusRecordInput.Builder.builder()
                .withDatabusRequestContext(new DatabusRequestContext("cloudera", null, null, null))
                .withPayload("{\"name\": \"value\"}")
                .withDatabusStreamConfiguration(monitoringConfiguration)
                .build();
    }
}
