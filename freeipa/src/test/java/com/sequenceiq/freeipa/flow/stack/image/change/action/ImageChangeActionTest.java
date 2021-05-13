package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.freeipa.flow.stack.image.change.action.ImageChangeActions.IMAGE_CHANGED_IN_DB;
import static com.sequenceiq.freeipa.flow.stack.image.change.action.ImageChangeActions.IMAGE_ENTITY_ID;
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

import org.hibernate.envers.AuditReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.service.image.ImageService;

import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class ImageChangeActionTest {

    @Mock
    private ImageService imageService;

    @Mock
    private AuditReader auditReader;

    @Mock
    private EventBus eventBus;

    @Mock
    private FlowRegister flowRegister;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private ImageChangeAction underTest;

    @Test
    public void testStoreRevision() throws Exception {
        StackContext stackContext = mock(StackContext.class);
        Stack stack = new Stack();
        when(stackContext.getStack()).thenReturn(stack);
        when(stackContext.getFlowParameters()).thenReturn(new FlowParameters("flid", "userCrn", null));
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setId(2L);
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);
        when(auditReader.getRevisions(ImageEntity.class, imageEntity.getId())).thenReturn(List.of(1, 2, 3));
        Map<Object, Object> variables = new HashMap<>();
        ImageSettingsRequest request = new ImageSettingsRequest();

        underTest.doExecute(stackContext, new ImageChangeEvent(1L, request), variables);

        assertEquals(Boolean.TRUE, variables.get(IMAGE_CHANGED_IN_DB));
        assertEquals(3, variables.get(ORIGINAL_IMAGE_REVISION));
        assertEquals(2L, variables.get(IMAGE_ENTITY_ID));
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
        when(stackContext.getFlowParameters()).thenReturn(new FlowParameters("flid", "userCrn", null));
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setId(2L);
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);
        when(auditReader.getRevisions(ImageEntity.class, imageEntity.getId())).thenReturn(List.of());
        Map<Object, Object> variables = new HashMap<>();
        ImageSettingsRequest request = new ImageSettingsRequest();

        underTest.doExecute(stackContext, new ImageChangeEvent(1L, request), variables);

        assertEquals(Boolean.TRUE, variables.get(IMAGE_CHANGED_IN_DB));
        assertFalse(variables.containsKey(ORIGINAL_IMAGE_REVISION));
        assertFalse(variables.containsKey(IMAGE_ENTITY_ID));
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
        when(stackContext.getFlowParameters()).thenReturn(new FlowParameters("flid", "userCrn", null));
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setId(2L);
        ImageSettingsRequest request = new ImageSettingsRequest();
        when(imageService.getByStackId(1L)).thenReturn(imageEntity);
        when(imageService.changeImage(stack, request)).thenReturn(imageEntity);
        when(auditReader.getRevisions(ImageEntity.class, imageEntity.getId())).thenReturn(List.of());
        Map<Object, Object> variables = new HashMap<>();

        underTest.doExecute(stackContext, new ImageChangeEvent(1L, request), variables);

        assertEquals(Boolean.TRUE, variables.get(IMAGE_CHANGED_IN_DB));
        assertFalse(variables.containsKey(ORIGINAL_IMAGE_REVISION));
        assertFalse(variables.containsKey(IMAGE_ENTITY_ID));
        assertEquals(imageEntity, variables.get(ORIGINAL_IMAGE));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        ImageChangeEvent event = (ImageChangeEvent) captor.getValue();
        assertEquals(IMAGE_CHANGE_NOT_REQUIRED_EVENT.event(), event.selector());
        assertEquals(1L, event.getResourceId());
        assertEquals(request, event.getRequest());
    }

}