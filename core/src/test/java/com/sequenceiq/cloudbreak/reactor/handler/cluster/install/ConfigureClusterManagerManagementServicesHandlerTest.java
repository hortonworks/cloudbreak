package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProviderProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@ExtendWith(MockitoExtension.class)
class ConfigureClusterManagerManagementServicesHandlerTest {

    private static final long STACK_ID = 1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private ClusterBuilderService clusterBuilderService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Mock
    private CentralCmTemplateUpdater centralCmTemplateUpdater;

    @Mock
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @InjectMocks
    private ConfigureClusterManagerManagementServicesHandler underTest;

    @Captor
    private ArgumentCaptor<Event<ConfigureClusterManagerManagementServicesSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<ConfigureClusterManagerManagementServicesFailed>> failedEventCaptor;

    @Test
    void testSelector() {
        assertEquals("CONFIGURECLUSTERMANAGERMANAGEMENTSERVICESREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        ConfigureClusterManagerManagementServicesFailed result = (ConfigureClusterManagerManagementServicesFailed) underTest
                .defaultFailureEvent(STACK_ID, e, new Event<>(new ConfigureClusterManagerManagementServicesRequest(STACK_ID)));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() throws IOException, CloudbreakException {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = mock();
        when(currentImage.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.12.0.400"));
        StatedImage targetStatedImage = mock();
        Image targetImage = mock();
        when(targetStatedImage.getImage()).thenReturn(targetImage);
        when(targetImage.getPackageVersions()).thenReturn(Map.of(CM.getKey(), "7.12.0.500"));
        StackDto stack = mock();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        TemplatePreparationObject templatePreparationObject = mock();
        when(stackToTemplatePreparationObjectConverter.convert(stack)).thenReturn(templatePreparationObject);
        CmTemplateProcessor cmTemplateProcessor = mock();
        when(centralCmTemplateUpdater.getCmTemplateProcessor(templatePreparationObject)).thenReturn(cmTemplateProcessor);
        ClusterApi clusterApi = mock();
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(cmTemplateComponentConfigProviderProcessor.getServiceConfigsToBeUpdatedDuringUpgrade(cmTemplateProcessor, templatePreparationObject,
                "7.12.0.400", "7.12.0.500")).thenReturn(Map.of("service1", Map.of("key1", "value1"), "service2", Map.of("key2", "value2")));

        underTest.accept(new Event<>(new ConfigureClusterManagerManagementServicesRequest(STACK_ID, currentImage, targetStatedImage)));

        verify(clusterBuilderService).configureManagementServices(STACK_ID);
        verify(clusterApi).updateServiceConfig("service1", Map.of("key1", "value1"));
        verify(clusterApi).updateServiceConfig("service2", Map.of("key2", "value2"));
        verify(eventBus).notify(eq("CONFIGURECLUSTERMANAGERMANAGEMENTSERVICESSUCCESS"), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptWhenFailure() {
        RuntimeException e = new RuntimeException("test");
        doThrow(e).when(clusterBuilderService).configureManagementServices(STACK_ID);

        underTest.accept(new Event<>(new ConfigureClusterManagerManagementServicesRequest(STACK_ID)));

        verify(eventBus).notify(eq("CONFIGURECLUSTERMANAGERMANAGEMENTSERVICESFAILED"), failedEventCaptor.capture());
        assertEquals(STACK_ID, failedEventCaptor.getValue().getData().getResourceId());
        assertEquals(e, failedEventCaptor.getValue().getData().getException());
    }
}
