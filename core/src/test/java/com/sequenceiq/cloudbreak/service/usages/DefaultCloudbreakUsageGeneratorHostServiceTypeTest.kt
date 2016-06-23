package com.sequenceiq.cloudbreak.service.usages

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import org.mockito.BDDMockito.given
import org.mockito.Matchers.any
import org.mockito.Matchers.anyCollection
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.util.ArrayList
import java.util.Arrays
import java.util.Date
import java.util.HashSet

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.repository.TemplateRepository
import com.sequenceiq.cloudbreak.service.ServiceTestUtils

class DefaultCloudbreakUsageGeneratorHostServiceTypeTest {

    @InjectMocks
    private var underTest: DefaultCloudbreakUsageGeneratorService? = null
    @Mock
    private val usageRepository: CloudbreakUsageRepository? = null
    @Mock
    private val eventRepository: CloudbreakEventRepository? = null
    @Mock
    private val stackUsageGenerator: StackUsageGenerator? = null
    @Mock
    private val stackRepository: StackRepository? = null
    @Mock
    private val templateRepository: TemplateRepository? = null

    @Before
    fun before() {
        underTest = DefaultCloudbreakUsageGeneratorService()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testGenerateUsagesShouldCallGeneratorOnAllEventsWhenUsagesHasNeverGenerated() {
        //GIVEN
        val events = ArrayList<CloudbreakEvent>()
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
    }

    @Test
    fun testGenerateUsagesShouldCallGeneratorOnLastDaysEventsWhenUsagesHasPreviouslyGenerated() {
        //GIVEN
        val events = ArrayList<CloudbreakEvent>()
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(1L)
        given(eventRepository!!.findAll(any<Specification>(Specification<Any>::class.java), any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Specification>(Specification<Any>::class.java), any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldDeleteStackAndTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsDeleted() {
        //GIVEN
        val events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", Date()))
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        val stack = ServiceTestUtils.createStack()
        val template = ServiceTestUtils.createTemplate(AWS)
        template.isDeleted = true
        val instanceGroup = InstanceGroup()
        instanceGroup.template = template
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup)
        stack.instanceGroups = instanceGroups
        stack.status = Status.DELETE_COMPLETED
        `when`(stackRepository!!.findById(1L)).thenReturn(stack)
        `when`(stackRepository.findAllStackForTemplate(template.id)).thenReturn(Arrays.asList(stack))
        given(eventRepository.findCloudbreakEventsForStack(any<Long>(Long::class.java))).willReturn(ArrayList<CloudbreakEvent>())
        doNothing().`when`(eventRepository).delete(anyCollection())
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
        verify(stackRepository).findById(1L)
        verify(stackRepository, times(1)).delete(stack)
        verify(stackRepository, times(1)).findAllStackForTemplate(template.id)
        verify<TemplateRepository>(templateRepository, times(1)).delete(template)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldNotDeleteStackWhenStackDoesNotExistWithId() {
        //GIVEN
        val events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", Date()))
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        val stack = ServiceTestUtils.createStack()
        `when`(stackRepository!!.findById(1L)).thenReturn(null)
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
        verify(stackRepository).findById(1L)
        verify(stackRepository, never()).delete(any<Stack>(Stack::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldNotDeleteStackWhenStackStatusIsNotDeleteCompleted() {
        //GIVEN
        val events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", Date()))
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        val stack = ServiceTestUtils.createStack()
        stack.status = Status.AVAILABLE
        `when`(stackRepository!!.findById(1L)).thenReturn(stack)
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
        verify(stackRepository).findById(1L)
        verify(stackRepository, never()).delete(stack)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldDeleteStackAndNotTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsNotDeleted() {
        //GIVEN
        val events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", Date()))
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        val stack = ServiceTestUtils.createStack()
        val template = ServiceTestUtils.createTemplate(AWS)
        val instanceGroup = InstanceGroup()
        instanceGroup.template = template
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup)
        stack.instanceGroups = instanceGroups
        stack.status = Status.DELETE_COMPLETED
        `when`(stackRepository!!.findById(1L)).thenReturn(stack)
        `when`(stackRepository.findAllStackForTemplate(template.id)).thenReturn(Arrays.asList(stack))
        given(eventRepository.findCloudbreakEventsForStack(any<Long>(Long::class.java))).willReturn(ArrayList<CloudbreakEvent>())
        doNothing().`when`(eventRepository).delete(anyCollection())
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
        verify(stackRepository).findById(1L)
        verify(stackRepository, times(1)).delete(stack)
        verify(stackRepository, times(1)).findAllStackForTemplate(template.id)
        verify<TemplateRepository>(templateRepository, never()).delete(template)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldDeleteStackAndNotTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsReferedByMoreStack() {
        //GIVEN
        val events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", Date()))
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        val stack = ServiceTestUtils.createStack()
        val template = ServiceTestUtils.createTemplate(AWS)
        template.isDeleted = true
        val instanceGroup = InstanceGroup()
        instanceGroup.template = template
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup)
        stack.instanceGroups = instanceGroups
        stack.status = Status.DELETE_COMPLETED
        `when`(stackRepository!!.findById(1L)).thenReturn(stack)
        `when`(stackRepository.findAllStackForTemplate(template.id)).thenReturn(Arrays.asList(stack, ServiceTestUtils.createStack()))
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
        verify(stackRepository).findById(1L)
        verify(stackRepository, times(1)).delete(stack)
        verify(stackRepository, times(1)).findAllStackForTemplate(template.id)
        verify<TemplateRepository>(templateRepository, never()).delete(template)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldDeleteStackAndNotTheRelatedTemplatesWhenTheStackStateIsDeleteCompletedAndTemplateIsDeleted() {
        //GIVEN
        val events = Arrays.asList(ServiceTestUtils.createEvent(1L, 3, "", Date()))
        val usages = ArrayList<CloudbreakUsage>()
        given(usageRepository!!.count()).willReturn(0L)
        given(eventRepository!!.findAll(any<Sort>(Sort::class.java))).willReturn(events)
        given(stackUsageGenerator!!.generate(events)).willReturn(usages)
        val template = ServiceTestUtils.createTemplate(AWS)
        template.isDeleted = true
        val stack = ServiceTestUtils.createStack()
        val instanceGroup = InstanceGroup()
        instanceGroup.template = template
        val instanceGroup2 = InstanceGroup()
        instanceGroup2.template = template
        val instanceGroups = HashSet<InstanceGroup>()
        instanceGroups.add(instanceGroup)
        instanceGroups.add(instanceGroup2)
        stack.instanceGroups = instanceGroups
        stack.status = Status.DELETE_COMPLETED
        `when`(stackRepository!!.findById(1L)).thenReturn(stack)
        `when`(stackRepository.findAllStackForTemplate(template.id)).thenReturn(Arrays.asList(stack))
        given(eventRepository.findCloudbreakEventsForStack(any<Long>(Long::class.java))).willReturn(ArrayList<CloudbreakEvent>())
        doNothing().`when`(eventRepository).delete(anyCollection())
        //WHEN
        underTest!!.generate()
        //THEN
        verify(eventRepository).findAll(any<Sort>(Sort::class.java))
        verify(usageRepository).save(usages)
        verify(stackRepository).findById(1L)
        verify(stackRepository, times(1)).delete(stack)
        verify(stackRepository, atLeast(1)).findAllStackForTemplate(template.id)
        verify<TemplateRepository>(templateRepository, atLeast(1)).delete(template)
    }
}