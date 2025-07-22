package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesValidationRequest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeleteVolumesValidationHandlerTest {

    private static final String COMPUTE = "compute";

    @Mock
    private StackDeleteVolumesRequest stackDeleteVolumesRequest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private CloudResource cloudResource;

    @InjectMocks
    private DeleteVolumesValidationHandler underTest;

    private void setUpMocks(boolean volumesPresent, Set<String> componentsInGroup) {
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        Volume volume = mock(Volume.class);
        doReturn(volumesPresent ? List.of(volume) : List.of()).when(volumeSetAttributes).getVolumes();
        doReturn(volumeSetAttributes).when(cloudResource).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        Resource resource = mock(Resource.class);
        doReturn(COMPUTE).when(stackDeleteVolumesRequest).getGroup();
        doReturn(COMPUTE).when(resource).getInstanceGroup();
        doReturn(ResourceType.AZURE_VOLUMESET).when(resource).getResourceType();
        StackDto stackDto = mock(StackDto.class);
        doReturn(Set.of(resource)).when(stackDto).getResources();
        doReturn(cloudResource).when(cloudResourceConverter).convert(resource);
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        Blueprint bp = mock(Blueprint.class);
        String bluePrintJsonTest = "blueprintTest";
        doReturn(bluePrintJsonTest).when(bp).getBlueprintJsonText();
        doReturn(bp).when(stackDto).getBlueprint();
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(bluePrintJsonTest);
        doReturn(componentsInGroup).when(cmTemplateProcessor).getComponentsInHostGroup(COMPUTE);
        doReturn(Map.of(COMPUTE, Set.of())).when(cmTemplateProcessor).getServiceComponentsByHostGroup();
    }

    @Test
    public void testDeleteVolumesValidationAction() {
        setUpMocks(true, Set.of("COMPUTE"));

        Selectable result = underTest.doAccept(createEvent());

        assertInstanceOf(DeleteVolumesRequest.class, result);
        DeleteVolumesRequest deleteVolumesRequest = (DeleteVolumesRequest) result;
        assertEquals(stackDeleteVolumesRequest, deleteVolumesRequest.getStackDeleteVolumesRequest());
        assertEquals(List.of(cloudResource), deleteVolumesRequest.getResourcesToBeDeleted());
        assertEquals(Set.of(), deleteVolumesRequest.getHostTemplateServiceComponents());
    }

    @Test
    public void testDeleteVolumesValidationFailureAction() {
        setUpMocks(true, Set.of("GATEWAY"));


        Selectable result = underTest.doAccept(createEvent());

        assertEquals("DELETEVOLUMESFAILEDEVENT_ERROR", result.getSelector());
        assertInstanceOf(DeleteVolumesFailedEvent.class, result);
        assertInstanceOf(BadRequestException.class, result.getException());
        assertEquals("Group compute request to be scaled, isn't compute specific group. The Non-compliant service list is: GATEWAY",
                result.getException().getMessage());

    }

    @Test
    public void testDeleteVolumesValidationFailureActionNoVolumes() {
        setUpMocks(false, Set.of("COMPUTE"));

        Selectable result = underTest.doAccept(createEvent());

        assertEquals("DELETEVOLUMESFAILEDEVENT_ERROR", result.getSelector());
        assertInstanceOf(DeleteVolumesFailedEvent.class, result);
        assertInstanceOf(BadRequestException.class, result.getException());
        assertEquals("There are no persistent volumes attached to compute instance group", result.getException().getMessage());

    }

    private HandlerEvent<DeleteVolumesValidationRequest> createEvent() {
        String selector = DELETE_VOLUMES_VALIDATION_HANDLER_EVENT.event();
        DeleteVolumesValidationRequest triggerEvent = new DeleteVolumesValidationRequest(selector, 1L, stackDeleteVolumesRequest);
        return new HandlerEvent<>(new Event<>(triggerEvent));
    }
}
