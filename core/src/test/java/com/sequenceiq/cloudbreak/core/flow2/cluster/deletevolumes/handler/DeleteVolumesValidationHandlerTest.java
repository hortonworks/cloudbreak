package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_VALIDATION_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesValidationRequest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
public class DeleteVolumesValidationHandlerTest {

    @Mock
    private StackDeleteVolumesRequest stackDeleteVolumesRequest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private DeleteVolumesValidationHandler underTest;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    @Test
    public void testDeleteVolumesValidationAction() throws Exception {
        String selector = DELETE_VOLUMES_VALIDATION_HANDLER_EVENT.event();
        DeleteVolumesValidationRequest triggerEvent = new DeleteVolumesValidationRequest(selector, 1L, stackDeleteVolumesRequest);
        Event event = new Event<>(triggerEvent);
        StackDto stackDto = mock(StackDto.class);
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        Blueprint bp = mock(Blueprint.class);
        doReturn(bp).when(stackDto).getBlueprint();
        String blueprintText = FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-instantiator.bp");
        doReturn(blueprintText).when(bp).getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(eq(blueprintText));
        underTest.accept(event);
        verify(eventBus).notify(selectorCaptor.capture(), any());
        assertEquals(DELETE_VOLUMES_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    public void testDeleteVolumesValidationFailureAction() throws Exception {
        String selector = DELETE_VOLUMES_VALIDATION_HANDLER_EVENT.event();
        DeleteVolumesValidationRequest triggerEvent = new DeleteVolumesValidationRequest(selector, 1L, stackDeleteVolumesRequest);
        doReturn("gateway").when(stackDeleteVolumesRequest).getGroup();
        Event event = new Event<>(triggerEvent);
        StackDto stackDto = mock(StackDto.class);
        doReturn(stackDto).when(stackDtoService).getById(eq(1L));
        Blueprint bp = mock(Blueprint.class);
        doReturn(bp).when(stackDto).getBlueprint();
        String blueprintText = FileReaderUtils.readFileFromClasspathQuietly("test/defaults/blueprints/blueprint-with-instantiator.bp");
        doReturn(blueprintText).when(bp).getBlueprintText();
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprintText);
        doReturn(cmTemplateProcessor).when(cmTemplateProcessorFactory).get(eq(blueprintText));
        underTest.accept(event);
        verify(eventBus).notify(selectorCaptor.capture(), any());
        assertEquals("DELETEVOLUMESFAILEDEVENT_ERROR", selectorCaptor.getValue());
    }
}
