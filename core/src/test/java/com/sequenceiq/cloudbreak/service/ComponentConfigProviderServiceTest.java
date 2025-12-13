package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.ComponentRepository;

@ExtendWith(MockitoExtension.class)
class ComponentConfigProviderServiceTest {

    private static final Long COMPONENT_ID = 1L;

    private static final long STACK_ID = 1L;

    private static final String RELEASE_VERSION = "release-version";

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private TransactionService transactionService = new TransactionService();

    @Mock
    private EntityManager entityManager;

    @Mock
    private AuditReader auditReader;

    @InjectMocks
    private ComponentConfigProviderService underTest;

    @Test
    void replaceImageComponentWithNew() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        Component original = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json("asdf"), stack);
        Component modified = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json("fdas"), stack);
        when(componentRepository.findComponentByStackIdComponentTypeName(eq(stack.getId()), eq(original.getComponentType()), eq(original.getName())))
                .thenReturn(Optional.of(original));
        underTest.replaceImageComponentWithNew(modified);
        ArgumentCaptor<Component> argument = ArgumentCaptor.forClass(Component.class);
        verify(componentRepository).save(argument.capture());
        assertEquals(modified.getAttributes(), argument.getValue().getAttributes());
    }

    @Test
    void testRestoreSecondToLastVersion() throws Exception {
        try (MockedStatic<AuditReaderFactory> mockedAuditReader = mockStatic(AuditReaderFactory.class)) {
            mockedAuditReader.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);

            Stack stack = new Stack();
            stack.setId(STACK_ID);

            Component original = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(),
                    new Json(Image.builder().withTags(Map.of(RELEASE_VERSION, "7.3.1")).build()), stack);
            original.setId(COMPONENT_ID);

            Component previous = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(),
                    new Json(Image.builder().withTags(Map.of(RELEASE_VERSION, "7.2.18")).build()), stack);

            doAnswer((Answer<Void>) invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(transactionService).required(any(Runnable.class));
            when(auditReader.getRevisions(Component.class, COMPONENT_ID)).thenReturn(List.of(1, 2, 3));
            when(auditReader.find(Component.class, COMPONENT_ID, 2)).thenReturn(previous);
            underTest.restoreSecondToLastVersion(original);
            verify(componentRepository).save(previous);
        }
    }

    @Test
    void testRestoreSecondToLastVersionNoRevertNeeded() throws Exception {
        try (MockedStatic<AuditReaderFactory> mockedAuditReader = mockStatic(AuditReaderFactory.class)) {
            mockedAuditReader.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);

            Stack stack = new Stack();
            stack.setId(STACK_ID);

            Component original = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(),
                    new Json(Image.builder().withTags(Map.of(RELEASE_VERSION, "7.2.18")).build()), stack);
            original.setId(COMPONENT_ID);

            Component previous = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(),
                    new Json(Image.builder().withTags(Map.of(RELEASE_VERSION, "7.3.1")).build()), stack);

            doAnswer((Answer<Void>) invocation -> {
                Runnable runnable = invocation.getArgument(0);
                runnable.run();
                return null;
            }).when(transactionService).required(any(Runnable.class));
            when(auditReader.getRevisions(Component.class, COMPONENT_ID)).thenReturn(List.of(1, 2, 3));
            when(auditReader.find(Component.class, COMPONENT_ID, 2)).thenReturn(previous);
            underTest.restoreSecondToLastVersion(original);
            verifyNoInteractions(componentRepository);
        }
    }
}