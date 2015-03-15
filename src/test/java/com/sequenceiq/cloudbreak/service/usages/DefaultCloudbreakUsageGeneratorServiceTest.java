package com.sequenceiq.cloudbreak.service.usages;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

public class DefaultCloudbreakUsageGeneratorServiceTest {

    @InjectMocks
    private DefaultCloudbreakUsageGeneratorService underTest;
    @Mock
    private CloudbreakUsageRepository usageRepository;
    @Mock
    private CloudbreakEventRepository eventRepository;
    @Mock
    private StackUsageGenerator stackUsageGenerator;
    @Mock
    private StackRepository stackRepository;
    @Mock
    private TemplateRepository templateRepository;

    @Before
    public void before() {
        underTest = new DefaultCloudbreakUsageGeneratorService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateUsagesShouldCallGeneratorOnAllEventsWhenUsagesHasNeverGenerated() {
        //GIVEN
        List<CloudbreakEvent> events = new ArrayList<>();
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
    }

    @Test
    public void testGenerateUsagesShouldCallGeneratorOnLastDaysEventsWhenUsagesHasPreviouslyGenerated() {
        //GIVEN
        List<CloudbreakEvent> events = new ArrayList<>();
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(1L);
        given(eventRepository.findAll(any(Specification.class), any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Specification.class), any(Sort.class));
        verify(usageRepository).save(usages);
    }

    @Test
    public void testGenerateShouldDeleteStackAndTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsDeleted() throws Exception {
        //GIVEN
        List<CloudbreakEvent> events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", new Date()));
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        Stack stack = ServiceTestUtils.createStack();
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        template.setDeleted(true);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup);
        stack.setInstanceGroups(instanceGroups);
        stack.setStatus(Status.DELETE_COMPLETED);
        when(stackRepository.findById(1L)).thenReturn(stack);
        when(stackRepository.findAllStackForTemplate(template.getId())).thenReturn(Arrays.asList(stack));
        given(eventRepository.findCloudbreakEventsForStack(any(Long.class))).willReturn(new ArrayList<CloudbreakEvent>());
        doNothing().when(eventRepository).delete(anyCollection());
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
        verify(stackRepository).findById(1L);
        verify(stackRepository, times(1)).delete(stack);
        verify(stackRepository, times(1)).findAllStackForTemplate(template.getId());
        verify(templateRepository, times(1)).delete(template);
    }

    @Test
    public void testGenerateShouldNotDeleteStackWhenStackDoesNotExistWithId() throws Exception {
        //GIVEN
        List<CloudbreakEvent> events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", new Date()));
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        Stack stack = ServiceTestUtils.createStack();
        when(stackRepository.findById(1L)).thenReturn(null);
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
        verify(stackRepository).findById(1L);
        verify(stackRepository, never()).delete(any(Stack.class));
    }

    @Test
    public void testGenerateShouldNotDeleteStackWhenStackStatusIsNotDeleteCompleted() throws Exception {
        //GIVEN
        List<CloudbreakEvent> events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", new Date()));
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.AVAILABLE);
        when(stackRepository.findById(1L)).thenReturn(stack);
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
        verify(stackRepository).findById(1L);
        verify(stackRepository, never()).delete(stack);
    }

    @Test
    public void testGenerateShouldDeleteStackAndNotTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsNotDeleted() throws Exception {
        //GIVEN
        List<CloudbreakEvent> events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", new Date()));
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        Stack stack = ServiceTestUtils.createStack();
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup);
        stack.setInstanceGroups(instanceGroups);
        stack.setStatus(Status.DELETE_COMPLETED);
        when(stackRepository.findById(1L)).thenReturn(stack);
        when(stackRepository.findAllStackForTemplate(template.getId())).thenReturn(Arrays.asList(stack));
        given(eventRepository.findCloudbreakEventsForStack(any(Long.class))).willReturn(new ArrayList<CloudbreakEvent>());
        doNothing().when(eventRepository).delete(anyCollection());
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
        verify(stackRepository).findById(1L);
        verify(stackRepository, times(1)).delete(stack);
        verify(stackRepository, times(1)).findAllStackForTemplate(template.getId());
        verify(templateRepository, never()).delete(template);
    }

    @Test
    public void testGenerateShouldDeleteStackAndNotTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsReferedByMoreStack() throws Exception {
        //GIVEN
        List<CloudbreakEvent> events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", new Date()));
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        Stack stack = ServiceTestUtils.createStack();
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        template.setDeleted(true);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup);
        stack.setInstanceGroups(instanceGroups);
        stack.setStatus(Status.DELETE_COMPLETED);
        when(stackRepository.findById(1L)).thenReturn(stack);
        when(stackRepository.findAllStackForTemplate(template.getId())).thenReturn(Arrays.asList(stack, ServiceTestUtils.createStack()));
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
        verify(stackRepository).findById(1L);
        verify(stackRepository, times(1)).delete(stack);
        verify(stackRepository, times(1)).findAllStackForTemplate(template.getId());
        verify(templateRepository, never()).delete(template);
    }

    @Test
    public void testGenerateShouldDeleteStackAndNotTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsDeleted() throws Exception {
        //GIVEN
        List<CloudbreakEvent> events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", new Date()));
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        template.setDeleted(true);
        Stack stack = ServiceTestUtils.createStack();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setTemplate(template);
        InstanceGroup instanceGroup2 = new InstanceGroup();
        instanceGroup2.setTemplate(template);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup);
        instanceGroups.add(instanceGroup2);
        stack.setInstanceGroups(instanceGroups);
        stack.setStatus(Status.DELETE_COMPLETED);
        when(stackRepository.findById(1L)).thenReturn(stack);
        when(stackRepository.findAllStackForTemplate(template.getId())).thenReturn(Arrays.asList(stack));
        given(eventRepository.findCloudbreakEventsForStack(any(Long.class))).willReturn(new ArrayList<CloudbreakEvent>());
        doNothing().when(eventRepository).delete(anyCollection());
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
        verify(stackRepository).findById(1L);
        verify(stackRepository, times(1)).delete(stack);
        verify(stackRepository, atLeast(1)).findAllStackForTemplate(template.getId());
        verify(templateRepository, atLeast(1)).delete(template);
    }
}