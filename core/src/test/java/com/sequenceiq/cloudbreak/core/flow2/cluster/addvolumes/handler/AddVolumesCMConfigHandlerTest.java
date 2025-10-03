package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AddVolumesCMConfigHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConfigUpdateUtilService configUpdateUtilService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @InjectMocks
    private AddVolumesCMConfigHandler underTest;

    private AddVolumesCMConfigHandlerEvent handlerRequest;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private StackDto stackDto;

    @Captor
    private ArgumentCaptor<BaseFailedFlowEvent> failedCaptor;

    @BeforeEach
    void setUp() {
        handlerRequest = new AddVolumesCMConfigHandlerEvent(STACK_ID, "test", 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL);
        doReturn(stackDto).when(stackDtoService).getById(eq(STACK_ID));
        doReturn("test").when(stackDto).getBlueprintJsonText();
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(eq("test"));
        Map<String, Set<ServiceComponent>> serviceComponentsMap = Map.of("test", Set.of(ServiceComponent.of("test", "test")));
        doReturn(serviceComponentsMap).when(cmTemplateProcessor).getServiceComponentsByHostGroup();
    }

    @Test
    void testAddVolumesCMConfigTest() throws Exception {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT.event(), response.getSelector());
        verify(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(), any(), eq("test"));
    }

    @Test
    void testAddVolumesCMConfigException() throws Exception {
        doThrow(new CloudbreakServiceException("TEST")).when(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(),
                any(), eq("test"));
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(FAILURE_EVENT.event(), response.getSelector());
        verify(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(), any(), eq("test"));
    }
}