package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.freeipa.flow.stack.image.change.action.ImageChangeActions.IMAGE_CHANGED_IN_DB;
import static com.sequenceiq.freeipa.flow.stack.image.change.action.ImageChangeActions.ORIGINAL_IMAGE;
import static com.sequenceiq.freeipa.flow.stack.image.change.action.ImageChangeActions.ORIGINAL_IMAGE_REVISION;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGED_IN_DB_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_NOT_REQUIRED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ImageChangeActionTest {

    private static final long IMAGE_ENTITY_ID = 2L;

    @Mock
    private ImageService imageService;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private ImageRevisionReaderService imageRevisionReaderService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private ImageChangeAction underTest;

    @Test
    public void testStoreRevision() throws Exception {
        StackContext stackContext = mock(StackContext.class);
        Stack stack = new Stack();
        when(stackContext.getStack()).thenReturn(stack);
        when(stackContext.getFlowParameters()).thenReturn(new FlowParameters("flid", "userCrn"));
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setId(IMAGE_ENTITY_ID);
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);
        when(imageRevisionReaderService.getRevisions(IMAGE_ENTITY_ID)).thenReturn(List.of(1, 2, 3));
        ImageSettingsRequest request = new ImageSettingsRequest();
        ImageEntity newImage = new ImageEntity();
        newImage.setId(IMAGE_ENTITY_ID);
        newImage.setImageId("newImageUUID");
        when(imageService.changeImage(stack, request)).thenReturn(newImage);
        Map<Object, Object> variables = new HashMap<>();

        underTest.doExecute(stackContext, new ImageChangeEvent(1L, request), variables);

        assertEquals(Boolean.TRUE, variables.get(IMAGE_CHANGED_IN_DB));
        assertEquals(3, variables.get(ORIGINAL_IMAGE_REVISION));
        assertEquals(IMAGE_ENTITY_ID, variables.get(ImageChangeActions.IMAGE_ENTITY_ID));
        assertFalse(variables.containsKey(ORIGINAL_IMAGE));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        ImageChangeEvent event = (ImageChangeEvent) captor.getValue();
        assertEquals(IMAGE_CHANGED_IN_DB_EVENT.event(), event.selector());
        assertEquals(1L, event.getResourceId());
        assertEquals(request, event.getRequest());
    }

    @Test
    public void testStoreImageEntity() throws Exception {
        StackContext stackContext = mock(StackContext.class);
        Stack stack = new Stack();
        when(stackContext.getStack()).thenReturn(stack);
        when(stackContext.getFlowParameters()).thenReturn(new FlowParameters("flid", "userCrn"));
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setId(2L);
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);
        when(imageRevisionReaderService.getRevisions(IMAGE_ENTITY_ID)).thenReturn(List.of());
        ImageEntity newImage = new ImageEntity();
        newImage.setId(IMAGE_ENTITY_ID);
        newImage.setImageId("newImageUUID");
        ImageSettingsRequest request = new ImageSettingsRequest();
        when(imageService.changeImage(stack, request)).thenReturn(newImage);

        Map<Object, Object> variables = new HashMap<>();

        underTest.doExecute(stackContext, new ImageChangeEvent(1L, request), variables);

        assertEquals(Boolean.TRUE, variables.get(IMAGE_CHANGED_IN_DB));
        assertFalse(variables.containsKey(ORIGINAL_IMAGE_REVISION));
        assertFalse(variables.containsKey(ImageChangeActions.IMAGE_ENTITY_ID));
        assertEquals(imageEntity, variables.get(ORIGINAL_IMAGE));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        ImageChangeEvent event = (ImageChangeEvent) captor.getValue();
        assertEquals(IMAGE_CHANGED_IN_DB_EVENT.event(), event.selector());
        assertEquals(1L, event.getResourceId());
        assertEquals(request, event.getRequest());
    }

    @Test
    public void testSameImage() throws Exception {
        StackContext stackContext = mock(StackContext.class);
        Stack stack = new Stack();
        when(stackContext.getStack()).thenReturn(stack);
        when(stackContext.getFlowParameters()).thenReturn(new FlowParameters("flid", "userCrn"));
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setId(2L);
        imageEntity.setImageId("imageUUID");
        ImageSettingsRequest request = new ImageSettingsRequest();
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);
        when(imageService.changeImage(stack, request)).thenReturn(imageEntity);
        when(imageRevisionReaderService.getRevisions(IMAGE_ENTITY_ID)).thenReturn(List.of());
        Map<Object, Object> variables = new HashMap<>();

        underTest.doExecute(stackContext, new ImageChangeEvent(1L, request), variables);

        assertEquals(Boolean.TRUE, variables.get(IMAGE_CHANGED_IN_DB));
        assertFalse(variables.containsKey(ORIGINAL_IMAGE_REVISION));
        assertFalse(variables.containsKey(ImageChangeActions.IMAGE_ENTITY_ID));
        assertEquals(imageEntity, variables.get(ORIGINAL_IMAGE));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        ImageChangeEvent event = (ImageChangeEvent) captor.getValue();
        assertEquals(IMAGE_CHANGE_NOT_REQUIRED_EVENT.event(), event.selector());
        assertEquals(1L, event.getResourceId());
        assertEquals(request, event.getRequest());
    }

}