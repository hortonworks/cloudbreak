package com.sequenceiq.cloudbreak.service.metering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.common.api.type.CdpResourceType;

@ExtendWith(MockitoExtension.class)
class MismatchedInstanceHandlerServiceTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String GROUP = "master";

    private static final Long STACK_ID = 1L;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CoreVerticalScaleService coreVerticalScaleService;

    @Mock
    private StackUpscaleService stackUpscaleService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudContextProvider cloudContextProvider;

    @Mock
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private MismatchedInstanceHandlerService underTest;

    @Test
    void handleMismatchingInstanceTypesShouldReturnWithoutErrorWhenNoMismatchingInstanceGroups() {
        underTest.handleMismatchingInstanceTypes(stack(), Set.of());
        verify(credentialClientService, never()).getExtendedCloudCredential(any());
        verify(metricService, never()).incrementMetricCounter(any(MetricType.class));
    }

    @Test
    void handleMismatchingInstanceTypesShouldThrowExceptionWhenGettingVmTypesFailed() {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenThrow(new RuntimeException("error"));

        StackDto stack = stack();
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "medium", Map.of("instanceId", "large")))));
        Assertions.assertEquals("error", runtimeException.getMessage());

        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, times(1)).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
    }

    @Test
    void handleMismatchingInstanceTypesShouldThrowExceptionWhenAvailableInstanceTypesDoesNotContainsOriginalInstanceType() {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());

        StackDto stack = stack();
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "unknown", Map.of("instanceId", "large")))));

        assertEquals("vmType 'unknown' not found.", notFoundException.getMessage());
        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, times(1)).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
    }

    @Test
    void handleMismatchingInstanceTypesShouldReturnWithoutErrorWhenOriginalInstanceTypeIsTheLargest() {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());

        StackDto stack = stack();
        underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "large", Map.of("instanceId", "medium"))));

        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL));
    }

    @Test
    void handleMismatchingInstanceTypesShouldReturnWithoutErrorWhenOriginalInstanceTypeIsTheSameAsNew() {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());

        StackDto stack = stack();
        underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "large", Map.of("instanceId", "large"))));

        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL));
    }

    @Test
    void handleMismatchingInstanceTypesShouldReturnWithoutErrorWhenOriginalInstanceTypeParametersAreTheSameThanNew() {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());

        StackDto stack = stack();
        underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "large", Map.of("instanceId", "largev2"))));

        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL));
    }

    @Test
    void handleMismatchingInstanceTypesShouldReturnWithoutErrorWhenOnlyTheCpuOrMemoryIsLargerThanTheOriginal() {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());

        StackDto stack = stack();
        underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "large", Map.of("instanceId", "large_11_900"))));

        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL));
    }

    @Test
    void handleMismatchingInstanceTypesShouldChangeInstanceTypeWhenNewIsLargerThanOriginal() throws Exception {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());
        StackDto stack = stack();
        when(cloudContextProvider.getCloudContext(eq(stack)))
                .thenReturn(CloudContext.Builder.builder().withPlatform("platform").withVariant("variant").build());
        when(credentialClientService.getCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(new CloudCredential());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), any())).thenReturn(mock(AuthenticatedContext.class));
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        CloudStack cloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(eq(stack))).thenReturn(cloudStack);
        when(stackToCloudStackConverter.updateWithVerticalScaleRequest(any(), any())).thenReturn(cloudStack);

        underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "medium", Map.of("instanceId", "large"))));

        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
        verify(metricService, times(1)).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL));
        verify(stackUpscaleService, times(1)).verticalScaleWithoutInstances(any(), any(), eq(cloudConnector), eq(GROUP));
        verify(coreVerticalScaleService, times(1)).updateTemplateWithVerticalScaleInformation(eq(STACK_ID), any());
    }

    @Test
    void handleMismatchingInstanceTypesShouldThrowExceptionWhenVerticalScaleOnProviderFails() throws Exception {
        when(credentialClientService.getExtendedCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(
                new ExtendedCloudCredential(new CloudCredential(), "cloudPlatform", null, null, null));
        when(cloudParameterService.getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any())).thenReturn(cloudVmTypes());
        StackDto stack = stack();
        when(cloudContextProvider.getCloudContext(eq(stack)))
                .thenReturn(CloudContext.Builder.builder().withPlatform("platform").withVariant("variant").build());
        when(credentialClientService.getCloudCredential(eq(ENVIRONMENT_CRN))).thenReturn(new CloudCredential());
        CloudConnector cloudConnector = mock(CloudConnector.class);
        Authenticator authenticator = mock(Authenticator.class);
        when(authenticator.authenticate(any(), any())).thenReturn(mock(AuthenticatedContext.class));
        when(cloudConnector.authentication()).thenReturn(authenticator);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        CloudStack cloudStack = mock(CloudStack.class);
        when(stackToCloudStackConverter.convert(eq(stack))).thenReturn(cloudStack);
        when(stackToCloudStackConverter.updateWithVerticalScaleRequest(any(), any())).thenReturn(cloudStack);
        when(stackUpscaleService.verticalScaleWithoutInstances(any(), any(), eq(cloudConnector), eq("master"))).thenThrow(new RuntimeException("error"));

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class, () -> underTest.handleMismatchingInstanceTypes(stack,
                Set.of(new MismatchingInstanceGroup(GROUP, "medium", Map.of("instanceId", "large")))));

        assertEquals("Vertical scale without instances on provider failed", cloudConnectorException.getMessage());
        verify(credentialClientService, times(1)).getExtendedCloudCredential(eq(ENVIRONMENT_CRN));
        verify(cloudParameterService, times(1)).getVmTypesV2(any(), any(), any(), eq(CdpResourceType.DATAHUB), any());
        verify(metricService, times(1)).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_FAILED));
        verify(metricService, never()).incrementMetricCounter(eq(MetricType.METERING_CHANGE_INSTANCE_TYPE_SUCCESSFUL));
        verify(stackUpscaleService, times(1)).verticalScaleWithoutInstances(any(), any(), eq(cloudConnector), eq(GROUP));
        verify(coreVerticalScaleService, never()).updateTemplateWithVerticalScaleInformation(eq(STACK_ID), any());
    }

    private StackDto stack() {
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getId()).thenReturn(STACK_ID);
        lenient().when(stackDto.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        lenient().when(stackDto.getResources()).thenReturn(Set.of());
        return stackDto;
    }

    private CloudVmTypes cloudVmTypes() {
        Map<String, Set<VmType>> cloudVmResponses = Map.of("eu-central-1a",
                Set.of(VmType.vmTypeWithMeta("large_11_900", vmTypeMeta(11, 900.0F), false),
                        VmType.vmTypeWithMeta("largev2", vmTypeMeta(10, 1000.0F), false),
                        VmType.vmTypeWithMeta("large", vmTypeMeta(10, 1000.0F), false),
                        VmType.vmTypeWithMeta("medium", vmTypeMeta(8, 800.0F), false),
                        VmType.vmTypeWithMeta("small", vmTypeMeta(1, 1.0F), false)));
        return new CloudVmTypes(cloudVmResponses, Map.of());
    }

    private VmTypeMeta vmTypeMeta(int cpu, float memory) {
        VmTypeMeta vmTypeMeta = new VmTypeMeta();
        Map<String, Object> properties = new HashMap<>();
        properties.put(VmTypeMeta.CPU, cpu);
        properties.put(VmTypeMeta.MEMORY, memory);
        vmTypeMeta.setProperties(properties);
        return vmTypeMeta;
    }
}