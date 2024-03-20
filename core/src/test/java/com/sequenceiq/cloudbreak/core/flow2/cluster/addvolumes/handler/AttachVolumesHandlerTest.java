package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ATTACH_VOLUMES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AttachVolumesHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private AddVolumesService addVolumesService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private TemplateService templateService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @InjectMocks
    private AttachVolumesHandler underTest;

    private AttachVolumesHandlerEvent handlerRequest;

    @BeforeEach
    void setUp() {
        handlerRequest = new AttachVolumesHandlerEvent(STACK_ID, 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL, "test");
        Stack stack = mock(Stack.class);
        doReturn(stack).when(stackService).getById(eq(STACK_ID));
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        Resource resource = mock(Resource.class);
        doReturn(List.of(resource)).when(resourceService).findAllByStackIdAndInstanceGroupAndResourceTypeIn(eq(STACK_ID),
                eq("test"), eq(List.of(ResourceType.AWS_VOLUMESET)));
    }

    @Test
    void testAttachVolumes() {
        VolumeTemplate volumeTemplateInTheDatabase = new VolumeTemplate();
        volumeTemplateInTheDatabase.setVolumeType("gp2");
        volumeTemplateInTheDatabase.setVolumeCount(1);
        volumeTemplateInTheDatabase.setVolumeSize(400);
        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        Template template = mock(Template.class);
        doReturn(TemporaryStorage.EPHEMERAL_VOLUMES).when(template).getTemporaryStorage();
        VolumeSetAttributes attributes = mock(VolumeSetAttributes.class);
        VolumeSetAttributes.Volume volume = mock(VolumeSetAttributes.Volume.class);
        doReturn("gp2").when(volume).getType();
        doReturn(400).when(volume).getSize();
        doReturn(List.of(volume, volume, volume)).when(attributes).getVolumes();
        doReturn(Optional.of(attributes)).when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));
        doReturn(Set.of(volumeTemplateInTheDatabase)).when(template).getVolumeTemplates();
        doReturn(template).when(instanceGroupView).getTemplate();
        doReturn(Optional.of(instanceGroupView)).when(instanceGroupService).findInstanceGroupViewByStackIdAndGroupName(eq(STACK_ID), eq("test"));
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(ATTACH_VOLUMES_FINISHED_EVENT.event(), response.getSelector());
        verify(addVolumesService).attachVolumes(any(), eq(STACK_ID));
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(templateService).savePure(templateCaptor.capture());
        Set<VolumeTemplate> volumeTemplatesSaved = templateCaptor.getValue().getVolumeTemplates();
        assertEquals(1, volumeTemplatesSaved.size());
        assertEquals(3, volumeTemplatesSaved.stream().findFirst().get().getVolumeCount());
    }

    @Test
    void testAttachVolumesException() throws CloudbreakServiceException {
        doThrow(new CloudbreakServiceException("TEST")).when(addVolumesService).attachVolumes(any(), eq(STACK_ID));
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(FAILURE_EVENT.event(), response.getSelector());
        verify(addVolumesService).attachVolumes(any(), eq(STACK_ID));
    }
}
