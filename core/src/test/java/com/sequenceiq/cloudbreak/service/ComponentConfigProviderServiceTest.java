package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;

@ExtendWith(MockitoExtension.class)
class ComponentConfigProviderServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @InjectMocks
    private ComponentConfigProviderService componentConfigProviderService;

    @Test
    void replaceImageComponentWithNew() {
        Stack stack = new Stack();
        stack.setId(1L);
        Component original = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json("asdf"), stack);
        Component modified = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json("fdas"), stack);
        when(componentRepository.findComponentByStackIdComponentTypeName(eq(stack.getId()), eq(original.getComponentType()), eq(original.getName())))
                .thenReturn(Optional.of(original));
        componentConfigProviderService.replaceImageComponentWithNew(modified);
        ArgumentCaptor<Component> argument = ArgumentCaptor.forClass(Component.class);
        verify(componentRepository).save(argument.capture());
        assertEquals(modified.getAttributes(), argument.getValue().getAttributes());
    }
}