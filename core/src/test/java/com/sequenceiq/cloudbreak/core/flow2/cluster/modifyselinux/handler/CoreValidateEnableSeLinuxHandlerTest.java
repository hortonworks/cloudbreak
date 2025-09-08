package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.FAILED_MODIFY_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.MODIFY_SELINUX_CORE_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class CoreValidateEnableSeLinuxHandlerTest {

    @Mock
    private StackService stackService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SeLinuxValidationService seLinuxValidationService;

    @InjectMocks
    private CoreValidateModifySeLinuxHandler underTest;

    private CoreValidateModifySeLinuxHandlerEvent event;

    @Mock
    private Stack stack;

    @Captor
    private ArgumentCaptor<Image> imageCaptor;

    @BeforeEach
    void setUp() {
        event = new CoreValidateModifySeLinuxHandlerEvent(1L, SeLinux.ENFORCING);
    }

    @Test
    void testValidateEnableSeLinuxHandlerSuccess() throws CloudbreakOrchestratorFailedException {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getResourceCrn()).thenReturn("test-crn");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        Map<String, String> selinuxModes = Map.of("host1", "permissive");
        when(hostOrchestrator.runCommandOnAllHosts(gatewayConfig, "getenforce")).thenReturn(selinuxModes);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c120\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"redhat8\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc7f\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null}"));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData));
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(1L, response.getResourceId());
        assertEquals(MODIFY_SELINUX_CORE_EVENT.selector(), response.getSelector());
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runCommandOnAllHosts(eq(gatewayConfig), eq("getenforce"));
        verify(seLinuxValidationService).validateSeLinuxEntitlementGranted(SeLinux.ENFORCING);
        verify(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(eq(SeLinux.ENFORCING), imageCaptor.capture());
        assertEquals("784cb6a3-5f54-4359-962e-c196897abc7f", imageCaptor.getValue().getImageId());
    }

    @Test
    void testValidateEnableSeLinuxHandlerImageValidationFailed() {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData1 = mock(InstanceMetaData.class);
        when(instanceMetaData1.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c120\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"redhat8\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc7f\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null}"));
        InstanceMetaData instanceMetaData2 = mock(InstanceMetaData.class);
        when(instanceMetaData2.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c121\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"redhat8\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc8a\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null}"));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData1, instanceMetaData2));
        CloudbreakRuntimeException exception  = assertThrows(CloudbreakRuntimeException.class, () ->
                underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("The images on the instances are different, please consider upgrading the instances to the same image.",
                exception.getMessage());
        verifyNoInteractions(gatewayConfigService);
        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    void testValidateEnableSeLinuxHandlerImageOSValidationFailed() {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c120\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"centos7\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc7f\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null}"));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData));
        CloudbreakRuntimeException exception  = assertThrows(CloudbreakRuntimeException.class, () ->
                underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("The centos7 OS installed on instances is not supported for SELinux 'ENFORCING' mode.",
                exception.getMessage());
        verifyNoInteractions(gatewayConfigService);
        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    void testValidateEnableSeLinuxHandlerSeLinuxModeValidationFailed() throws CloudbreakOrchestratorFailedException {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getResourceCrn()).thenReturn("test-crn");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        Map<String, String> selinuxModes = Map.of("host1", "disabled");
        when(hostOrchestrator.runCommandOnAllHosts(gatewayConfig, "getenforce")).thenReturn(selinuxModes);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c120\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"redhat8\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc7f\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null}"));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData));
        CloudbreakRuntimeException exception  = assertThrows(CloudbreakRuntimeException.class, () ->
                underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("SeLinux mode for some instances are in 'disabled' mode.", exception.getMessage());
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runCommandOnAllHosts(eq(gatewayConfig), eq("getenforce"));
    }

    @Test
    void testValidateEnableSeLinuxHandlerSeLinuxModeValidationOrchestratorFailed() throws CloudbreakOrchestratorFailedException {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        when(stack.getResourceCrn()).thenReturn("test-crn");
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(gatewayConfig);
        when(hostOrchestrator.runCommandOnAllHosts(gatewayConfig, "getenforce")).thenThrow(new CloudbreakOrchestratorFailedException("test"));
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c120\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"redhat8\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc7f\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null}"));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData));
        CloudbreakRuntimeException exception  = assertThrows(CloudbreakRuntimeException.class, () ->
                underTest.doAccept(new HandlerEvent<>(new Event<>(event))));
        assertEquals("Unable to validate SELinux modes - connection to instances failed.", exception.getMessage());
        verify(gatewayConfigService).getPrimaryGatewayConfig(stack);
        verify(hostOrchestrator).runCommandOnAllHosts(eq(gatewayConfig), eq("getenforce"));
    }

    @Test
    void testValidateEnableSeLinuxHandlerFailedBecauseNotEntitled() {
        doThrow(CloudbreakServiceException.class).when(seLinuxValidationService).validateSeLinuxEntitlementGranted(SeLinux.ENFORCING);

        assertThrows(CloudbreakServiceException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));

        verifyNoInteractions(stackService);
        verifyNoInteractions(gatewayConfigService);
        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    void testValidateEnableSeLinuxHandlerFailedBecauseImageDoesNotSupportEnforcing() {
        when(stackService.getByIdWithListsInTransaction(1L)).thenReturn(stack);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getImage()).thenReturn(new Json("{\"imageName\":\"ami-03f1c0e9e1e12c120\",\"userdata\":{\"CORE\":\"\",\"GATEWAY\":\"\"}," +
                "\"os\":\"redhat8\",\"osType\":\"redhat8\"," +
                "\"imageCatalogUrl\":\"https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-image-catalog.json\",\"imageCatalogName\":null," +
                "\"imageId\":\"784cb6a3-5f54-4359-962e-c196897abc7f\",\"packageVersions\":{\"source-image\":\"ami-039ce2eddc1949546\"}," +
                "\"date\":\"2023-11-23\",\"created\":null,\"tags\":{\"selinux-supported\":\"false\"}}"));
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(Set.of(instanceMetaData));
        doThrow(CloudbreakServiceException.class)
                .when(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(eq(SeLinux.ENFORCING), any(Image.class));

        assertThrows(CloudbreakServiceException.class, () -> underTest.doAccept(new HandlerEvent<>(new Event<>(event))));

        verify(seLinuxValidationService).validateSeLinuxEntitlementGranted(SeLinux.ENFORCING);
        verify(seLinuxValidationService).validateSeLinuxSupportedOnTargetImage(eq(SeLinux.ENFORCING), imageCaptor.capture());
        assertEquals("784cb6a3-5f54-4359-962e-c196897abc7f", imageCaptor.getValue().getImageId());
        assertThat(imageCaptor.getValue().getTags()).containsEntry("selinux-supported", "false");
        verifyNoInteractions(gatewayConfigService);
        verifyNoInteractions(hostOrchestrator);
    }

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(CoreValidateModifySeLinuxHandlerEvent.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(1L, new Exception("test"), new Event<>(event));
        assertEquals(FAILED_MODIFY_SELINUX_CORE_EVENT.selector(), response.getSelector());
        assertEquals("test", response.getException().getMessage());
    }
}
