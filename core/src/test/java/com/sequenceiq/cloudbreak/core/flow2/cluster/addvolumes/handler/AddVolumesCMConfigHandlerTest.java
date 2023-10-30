package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
public class AddVolumesCMConfigHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ConfigUpdateUtilService configUpdateUtilService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private AddVolumesCMConfigHandler underTest;

    private AddVolumesCMConfigHandlerEvent handlerRequest;

    @Mock
    private EventSender eventSender;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private StackDto stackDto;

    @Captor
    private ArgumentCaptor<BaseFlowEvent> captor;

    @Captor
    private ArgumentCaptor<BaseFailedFlowEvent> failedCaptor;

    @BeforeEach
    public void setUp() {
        underTest = new AddVolumesCMConfigHandler(eventSender);
        ReflectionTestUtils.setField(underTest, null, stackDtoService, StackDtoService.class);
        ReflectionTestUtils.setField(underTest, null, cmTemplateProcessorFactory, CmTemplateProcessorFactory.class);
        ReflectionTestUtils.setField(underTest, null, configUpdateUtilService, ConfigUpdateUtilService.class);
        handlerRequest = new AddVolumesCMConfigHandlerEvent(STACK_ID, "test", 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL);
        doReturn(stackDto).when(stackDtoService).getById(eq(STACK_ID));
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        Template template = mock(Template.class);
        doReturn("test").when(instanceGroupView).getGroupName();
        doReturn(2).when(template).getInstanceStorageCount();
        doReturn(TemporaryStorage.EPHEMERAL_VOLUMES).when(template).getTemporaryStorage();
        doReturn(template).when(instanceGroupView).getTemplate();
        doReturn(List.of(instanceGroupView)).when(stackDto).getInstanceGroupViews();
        Blueprint bp = mock(Blueprint.class);
        doReturn("test").when(bp).getBlueprintJsonText();
        doReturn(bp).when(stackDto).getBlueprint();
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(eq("test"));
        Map<String, Set<ServiceComponent>> serviceComponentsMap = Map.of("test", Set.of(ServiceComponent.of("test", "test")));
        doReturn(serviceComponentsMap).when(cmTemplateProcessor).getServiceComponentsByHostGroup();
    }

    @Test
    public void testAddVolumesCMConfigTest() throws Exception {
        doReturn(Set.of("IMPALAD")).when(cmTemplateProcessor).getComponentsInHostGroup("test");
        underTest.accept(new Event<>(handlerRequest));
        verify(eventSender).sendEvent(captor.capture(), any());
        assertEquals(ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT.event(), captor.getValue().getSelector());
        verify(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(), eq(2), anyInt(), any(),
                eq(TemporaryStorage.EPHEMERAL_VOLUMES));
    }

    @Test
    public void testAddVolumesCMConfigBlacklistedServiceTest() throws Exception {
        doReturn(Set.of("KUDU_MASTER")).when(cmTemplateProcessor).getComponentsInHostGroup("test");
        underTest.accept(new Event<>(handlerRequest));
        verify(eventSender).sendEvent(captor.capture(), any());
        assertEquals(ADD_VOLUMES_CM_CONFIGURATION_FINISHED_EVENT.event(), captor.getValue().getSelector());
        verify(configUpdateUtilService, times(0)).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(), eq(2), anyInt(), any(),
                eq(TemporaryStorage.EPHEMERAL_VOLUMES));
    }

    @Test
    public void testAddVolumesCMConfigException() throws Exception {
        doReturn(Set.of("IMPALAD")).when(cmTemplateProcessor).getComponentsInHostGroup("test");
        doThrow(new CloudbreakServiceException("TEST")).when(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(),
                eq(2), anyInt(), any(), eq(TemporaryStorage.EPHEMERAL_VOLUMES));
        underTest.accept(new Event<>(handlerRequest));
        verify(eventSender).sendEvent(failedCaptor.capture(), any());
        assertEquals(FAILURE_EVENT.event(), failedCaptor.getValue().getSelector());
        verify(configUpdateUtilService).updateCMConfigsForComputeAndStartServices(eq(stackDto), any(), eq(2), anyInt(), any(),
                eq(TemporaryStorage.EPHEMERAL_VOLUMES));
    }
}
