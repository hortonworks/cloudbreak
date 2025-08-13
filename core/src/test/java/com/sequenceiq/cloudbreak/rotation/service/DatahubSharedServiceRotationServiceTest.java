package com.sequenceiq.cloudbreak.rotation.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CONFIGURATION_UPDATE_FAILED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class DatahubSharedServiceRotationServiceTest {

    @Mock
    private StackDtoService stackService;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private ClusterServicesRestartService clusterServicesRestartService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private DatahubSharedServiceRotationService underTest;

    private final StackDto datalake = mock(StackDto.class);

    private final StackDto datahub = mock(StackDto.class);

    @BeforeEach
    void setup() {
        lenient().doNothing().when(flowMessageService).fireEventAndLog(any(), any(), any(), any());
        lenient().doNothing().when(flowMessageService).fireEventAndLog(any(), any(), any());
        lenient().when(stackUpdater.updateStackStatus(any(), any())).thenReturn(mock(Stack.class));
        lenient().when(datahub.getId()).thenReturn(2L);
        lenient().when(datahub.getResourceCrn()).thenReturn("datahub");
        lenient().when(datahub.getBlueprint()).thenReturn(new Blueprint());
        lenient().when(datalake.getEnvironmentCrn()).thenReturn("env");
        lenient().when(datalake.getId()).thenReturn(1L);
        lenient().when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
    }

    @Test
    void testSuccessfulConfigUpdate() throws CloudbreakException {
        when(stackService.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(datahub));
        when(cmTemplateProcessor.isServiceTypePresent(any())).thenReturn(Boolean.TRUE);
        doNothing().when(clusterHostServiceRunner).updateClusterConfigs(any(), anyBoolean());
        doNothing().when(clusterServicesRestartService).refreshCluster(any());

        underTest.updateAllRelevantDatahub(datalake);

        verify(clusterServicesRestartService).refreshCluster(any());
        verify(clusterHostServiceRunner).updateClusterConfigs(any(), anyBoolean());
        verify(stackUpdater).updateStackStatus(eq(2L), eq(AVAILABLE));
    }

    @Test
    void testSkippedConfigUpdate() throws CloudbreakException {
        when(stackService.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(datahub));
        when(cmTemplateProcessor.isServiceTypePresent(any())).thenReturn(Boolean.FALSE);

        underTest.updateAllRelevantDatahub(datalake);

        verify(clusterServicesRestartService, times(0)).refreshCluster(any());
        verifyNoInteractions(clusterHostServiceRunner, stackUpdater);
    }

    @Test
    void testFailedConfigUpdate() throws CloudbreakException {
        when(stackService.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(datahub));
        when(cmTemplateProcessor.isServiceTypePresent(any())).thenReturn(Boolean.TRUE);
        doThrow(new RuntimeException("failed")).when(clusterHostServiceRunner).updateClusterConfigs(any(), anyBoolean());

        underTest.updateAllRelevantDatahub(datalake);

        verify(clusterServicesRestartService, times(0)).refreshCluster(any());
        verify(clusterHostServiceRunner).updateClusterConfigs(any(), anyBoolean());
        verify(stackUpdater).updateStackStatus(eq(2L), eq(CONFIGURATION_UPDATE_FAILED));
    }

    @Test
    void testSuccessfulDatahubStatusValidation() {
        when(cmTemplateProcessor.isServiceTypePresent(any())).thenReturn(Boolean.TRUE);
        when(stackService.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(datahub));
        when(datahub.getStatus()).thenReturn(Status.AVAILABLE);

        underTest.validateAllDatahubAvailable(datalake);

        verify(datahub).getStatus();
    }

    @Test
    void testSuccessfulDatahubStatusValidationIfStatusInvalidButHmsNotPresent() {
        when(stackService.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(datahub));
        when(cmTemplateProcessor.isServiceTypePresent(any())).thenReturn(Boolean.FALSE);

        underTest.validateAllDatahubAvailable(datalake);

        verify(datahub, times(0)).getStatus();
    }

    @Test
    void testFailedfulDatahubStatusValidation() {
        when(stackService.findAllByEnvironmentCrnAndStackType(any(), any())).thenReturn(List.of(datahub));
        when(cmTemplateProcessor.isServiceTypePresent(any())).thenReturn(Boolean.TRUE);
        when(datahub.getStatus()).thenReturn(Status.UPDATE_IN_PROGRESS);

        assertThrows(SecretRotationException.class, () -> underTest.validateAllDatahubAvailable(datalake));
    }

}
