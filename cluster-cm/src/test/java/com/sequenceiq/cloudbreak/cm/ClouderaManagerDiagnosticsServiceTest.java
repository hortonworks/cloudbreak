package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCollectDiagnosticDataArguments;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.CmDiagnosticsParameters;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerDiagnosticsServiceTest {

    private static final String CLUSTER_NAME = "test-cluster";

    private static final Date START_TIME = new Date(1_600_000_000_000L);

    private static final Date END_TIME = new Date(1_600_003_600_000L);

    @Mock
    private StackDtoDelegate stack;

    @Mock
    private HttpClientConfig clientConfig;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Mock
    private ClouderaManagerResourceApi resourceApi;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ExtendedPollingResult pollingResult;

    @Captor
    private ArgumentCaptor<ApiCollectDiagnosticDataArguments> argsCaptor;

    @Captor
    private ArgumentCaptor<ApiConfigList> configCaptor;

    private ClouderaManagerDiagnosticsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new ClouderaManagerDiagnosticsService(stack, clientConfig);
        ReflectionTestUtils.setField(underTest, "clouderaManagerApiFactory", clouderaManagerApiFactory);
        ReflectionTestUtils.setField(underTest, "clouderaManagerPollingServiceProvider",
                clouderaManagerPollingServiceProvider);
        ReflectionTestUtils.setField(underTest, "client", apiClient);
    }

    @Test
    void testCollectDiagnosticsMapsStartAndEndTimeToTheirOwnFields() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();
        setupMocks();

        underTest.collectDiagnostics(parameters);

        ApiCollectDiagnosticDataArguments captured = argsCaptor.getValue();
        assertEquals(new DateTime(START_TIME).toString(), captured.getStartTime());
        assertEquals(new DateTime(END_TIME).toString(), captured.getEndTime());
        assertEquals(CLUSTER_NAME, captured.getClusterName());
    }

    @Test
    void testCollectDiagnosticsWithNullStartTimeLeavesStartTimeUnset() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withStartTime(null)
                .withEndTime(END_TIME)
                .build();
        setupMocks();

        underTest.collectDiagnostics(parameters);

        ApiCollectDiagnosticDataArguments captured = argsCaptor.getValue();
        // null startTime is left unset so CM applies its own default, while endTime is still mapped
        assertNull(captured.getStartTime());
        assertEquals(new DateTime(END_TIME).toString(), captured.getEndTime());
    }

    @Test
    void testCollectDiagnosticsWithNullEndTimeDefaultsEndTimeToNow() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withStartTime(START_TIME)
                .withEndTime(null)
                .build();
        setupMocks();

        underTest.collectDiagnostics(parameters);

        ApiCollectDiagnosticDataArguments captured = argsCaptor.getValue();
        assertEquals(new DateTime(START_TIME).toString(), captured.getStartTime());
        // null endTime falls back to current time
        assertNotNull(captured.getEndTime());
    }

    @Test
    void testCollectDiagnosticsRestoresPhoneHomeWhenCommandFails() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withDestination(DiagnosticsDestination.CLOUD_STORAGE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(resourceApi);
        ApiException apiException = new ApiException("command failed");
        when(resourceApi.collectDiagnosticDataCommand(any())).thenThrow(apiException);

        ClouderaManagerOperationFailedException thrown = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.collectDiagnostics(parameters));

        assertEquals(apiException, thrown.getCause());
        InOrder inOrder = inOrder(resourceApi);
        inOrder.verify(resourceApi).getConfig(null);
        inOrder.verify(resourceApi).updateConfig(configCaptor.capture(), any());
        inOrder.verify(resourceApi).collectDiagnosticDataCommand(any());
        inOrder.verify(resourceApi).updateConfig(configCaptor.capture(), any());
        List<ApiConfigList> values = configCaptor.getAllValues();
        assertEquals("false", values.get(0).getItems().get(0).getValue());
        assertEquals("true", values.get(1).getItems().get(0).getValue());
    }

    @Test
    void testCollectDiagnosticsRestoresPhoneHomeWhenPollingTimesOut() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withDestination(DiagnosticsDestination.CLOUD_STORAGE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(resourceApi);
        when(resourceApi.collectDiagnosticDataCommand(any())).thenReturn(new ApiCommand().id(1L));
        when(clouderaManagerPollingServiceProvider.startPollingCollectDiagnostics(any(), any(), any())).thenReturn(pollingResult);
        when(pollingResult.isTimeout()).thenReturn(true);

        assertThrows(CloudbreakException.class, () -> underTest.collectDiagnostics(parameters));

        verify(resourceApi, times(2)).updateConfig(any(), any());
    }

    @Test
    void testCollectDiagnosticsRestoreFailureDoesNotMaskOriginalException() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withDestination(DiagnosticsDestination.CLOUD_STORAGE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(resourceApi);
        ApiException apiException = new ApiException("command failed");
        when(resourceApi.collectDiagnosticDataCommand(any())).thenThrow(apiException);
        when(resourceApi.updateConfig(any(), any())).thenReturn(new ApiConfigList()).thenThrow(new RuntimeException("restore failed"));

        ClouderaManagerOperationFailedException thrown = assertThrows(ClouderaManagerOperationFailedException.class,
                () -> underTest.collectDiagnostics(parameters));

        assertEquals(apiException, thrown.getCause());
        verify(resourceApi, times(2)).updateConfig(any(), any());
    }

    @Test
    void testCollectDiagnosticsHappyPathRestoresPhoneHomeOnce() throws Exception {
        CmDiagnosticsParameters parameters = CmDiagnosticsParameters.builder()
                .withClusterName(CLUSTER_NAME)
                .withDestination(DiagnosticsDestination.CLOUD_STORAGE)
                .withStartTime(START_TIME)
                .withEndTime(END_TIME)
                .build();
        setupMocks();

        underTest.collectDiagnostics(parameters);

        verify(resourceApi, times(2)).updateConfig(any(), any());
    }

    private void setupMocks() throws Exception {
        when(clouderaManagerApiFactory.getClouderaManagerResourceApi(any())).thenReturn(resourceApi);
        when(resourceApi.collectDiagnosticDataCommand(argsCaptor.capture())).thenReturn(new ApiCommand().id(1L));
        when(clouderaManagerPollingServiceProvider.startPollingCollectDiagnostics(any(), any(), any())).thenReturn(pollingResult);
    }
}
