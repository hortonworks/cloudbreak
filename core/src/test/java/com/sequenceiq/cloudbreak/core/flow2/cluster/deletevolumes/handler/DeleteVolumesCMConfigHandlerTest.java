package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_CM_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_FAIL_HANDLED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesCMConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DeleteVolumesCMConfigHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ConfigUpdateUtilService configUpdateUtilService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DeleteVolumesService deleteVolumesService;

    @InjectMocks
    private DeleteVolumesCMConfigHandler underTest;

    private Event<DeleteVolumesCMConfigEvent> handlerEvent;

    @Mock
    private StackDto stackDto;

    @Mock
    private CmTemplateProcessor processor;

    @BeforeEach
    void setUp() {
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        handlerEvent = new Event<>(new DeleteVolumesCMConfigEvent(STACK_ID, "test"));
        doReturn("test blueprint").when(stackDto).getBlueprintJsonText();
        doReturn(processor).when(cmTemplateProcessorFactory).get(any());
    }

    @Test
    void testCMConfigChangeHandler() throws Exception {
        Set<ServiceComponent> serviceComponents = Set.of(ServiceComponent.of("YARN", "YARN"));
        Set<String> componentsInHostGroup = Set.of("YARN");
        doReturn(Map.of("test", serviceComponents)).when(processor).getServiceComponentsByHostGroup();
        doReturn(componentsInHostGroup).when(processor).getComponentsInHostGroup("test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(configUpdateUtilService, times(1)).updateCMConfigsForComputeAndStartServices(eq(stackDto),
                eq(serviceComponents), anyList(), eq("test"));
        verify(deleteVolumesService, times(1)).startClouderaManagerService(stackDto, serviceComponents);
        assertEquals(DELETE_VOLUMES_CM_CONFIG_FINISHED_EVENT.event(), response.getSelector());
    }

    @Test
    void testCMConfigChangeHandlerForBlacklistedService() throws Exception {
        Set<ServiceComponent> serviceComponents = Set.of(ServiceComponent.of("KUDU_TSERVER", "KUDU_TSERVER"));
        Set<String> componentsInHostGroup = Set.of("KUDU_TSERVER");
        doReturn(Map.of("test", serviceComponents)).when(processor).getServiceComponentsByHostGroup();
        doReturn(componentsInHostGroup).when(processor).getComponentsInHostGroup("test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(configUpdateUtilService, times(0)).updateCMConfigsForComputeAndStartServices(eq(stackDto), anySet(),
                anyList(), eq("test"));
        verify(deleteVolumesService, times(1)).startClouderaManagerService(stackDto, serviceComponents);
        assertEquals(DELETE_VOLUMES_CM_CONFIG_FINISHED_EVENT.event(), response.getSelector());
    }

    @Test
    void testCMConfigChangeHandlerException() throws Exception {
        Set<ServiceComponent> serviceComponents = Set.of(ServiceComponent.of("KUDU_TSERVER", "KUDU_TSERVER"));
        Set<String> componentsInHostGroup = Set.of("KUDU_TSERVER");
        doReturn(Map.of("test", serviceComponents)).when(processor).getServiceComponentsByHostGroup();
        doReturn(componentsInHostGroup).when(processor).getComponentsInHostGroup("test");
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(deleteVolumesService).startClouderaManagerService(stackDto, serviceComponents);
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(configUpdateUtilService, times(0)).updateCMConfigsForComputeAndStartServices(eq(stackDto), anySet(),
                anyList(), eq("test"));
        verify(deleteVolumesService, times(1)).startClouderaManagerService(stackDto, serviceComponents);
        assertEquals(DELETE_VOLUMES_FAIL_HANDLED_EVENT.event(), response.getSelector());
    }
}
