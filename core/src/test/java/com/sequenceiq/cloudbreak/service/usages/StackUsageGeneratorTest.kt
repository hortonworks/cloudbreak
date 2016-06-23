package com.sequenceiq.cloudbreak.service.usages

import java.util.Calendar.DATE
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mockito.Matchers.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Calendar
import java.util.Date

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.common.type.BillingStatus
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository
import com.sequenceiq.cloudbreak.service.ServiceTestUtils

class StackUsageGeneratorTest {

    @InjectMocks
    private var underTest: StackUsageGenerator? = null
    @Mock
    private val eventRepository: CloudbreakEventRepository? = null
    @Mock
    private val intervalUsageGenerator: IntervalStackUsageGenerator? = null
    @Mock
    private val cloudbreakUsage: CloudbreakUsage? = null

    private var referenceCalendar: Calendar? = null

    @Before
    fun before() {
        val referenceDateStr = "2014-09-24"
        try {
            referenceCalendar = Calendar.getInstance()
            referenceCalendar!!.time = DATE_FORMAT.parse(referenceDateStr)
        } catch (e: ParseException) {
            LOGGER.error("invalid reference date str: {}, ex: {}", referenceDateStr, e)
        }

        underTest = StackUsageGenerator()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldEmptyLIstWhenNoEventExists() {

        val usageList = underTest!!.generate(ArrayList<CloudbreakEvent>())

        assertTrue(usageList.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldCreateExactUsageWhenStartAndStopEventsExist() {
        //GIVEN
        val startDate = referenceCalendar!!.time
        referenceCalendar!!.set(DATE, referenceCalendar!!.get(DATE) + 1)
        val stopDate = referenceCalendar!!.time
        val startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name, startDate)
        val stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name, stopDate)
        val usagesByDay = Arrays.asList<CloudbreakUsage>(cloudbreakUsage)
        `when`(intervalUsageGenerator!!.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay)
        val stack = ServiceTestUtils.createStack()
        stack.status = Status.AVAILABLE

        //WHEN
        val usageList = underTest!!.generate(Arrays.asList(startEvent, stopEvent))

        //THEN
        verify(intervalUsageGenerator).generateUsages(startDate, stopDate, startEvent)
        assertFalse(usageList.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldCreateExactUsageWhenStopEventDoesNotExist() {
        //GIVEN
        val startDate = referenceCalendar!!.time
        referenceCalendar!!.set(DATE, referenceCalendar!!.get(DATE) + 1)
        val startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name, startDate)
        val usagesByDay = Arrays.asList<CloudbreakUsage>(cloudbreakUsage)
        `when`(intervalUsageGenerator!!.generateUsages(any<Date>(Date::class.java), any<Date>(Date::class.java), any<CloudbreakEvent>(CloudbreakEvent::class.java))).thenReturn(usagesByDay)
        val stack = ServiceTestUtils.createStack()
        stack.status = Status.AVAILABLE

        //WHEN
        val usageList = underTest!!.generate(Arrays.asList(startEvent))

        //THEN
        assertFalse(usageList.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldCreateNewBillingStartEventWhenStopEventDoesNotExist() {
        //GIVEN
        val startDate = referenceCalendar!!.time
        referenceCalendar!!.set(DATE, referenceCalendar!!.get(DATE) + 1)
        val startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name, startDate)
        val usagesByDay = Arrays.asList<CloudbreakUsage>(cloudbreakUsage)
        `when`(intervalUsageGenerator!!.generateUsages(any<Date>(Date::class.java), any<Date>(Date::class.java), any<CloudbreakEvent>(CloudbreakEvent::class.java))).thenReturn(usagesByDay)
        val stack = ServiceTestUtils.createStack()
        stack.status = Status.AVAILABLE

        //WHEN
        val usageList = underTest!!.generate(Arrays.asList(startEvent))

        //THEN
        verify<CloudbreakEventRepository>(eventRepository, times(1)).save(any<CloudbreakEvent>(CloudbreakEvent::class.java))
        assertFalse(usageList.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldReturnEmptyListWhenStartEventDoesNotExist() {
        //GIVEN
        val startDate = referenceCalendar!!.time
        referenceCalendar!!.set(DATE, referenceCalendar!!.get(DATE) + 1)
        val stopDate = referenceCalendar!!.time
        val startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name, startDate)
        val stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name, stopDate)
        val usagesByDay = Arrays.asList<CloudbreakUsage>(cloudbreakUsage)
        `when`(intervalUsageGenerator!!.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay)
        val stack = ServiceTestUtils.createStack()
        stack.status = Status.AVAILABLE

        //WHEN
        val usageList = underTest!!.generate(Arrays.asList(startEvent, stopEvent))

        //THEN
        assertTrue(usageList.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateShouldConsiderTheFirstStartWhenMoreStartEventsExist() {
        //GIVEN
        val startDate = referenceCalendar!!.time
        referenceCalendar!!.set(DATE, referenceCalendar!!.get(DATE) + 1)
        val secondStartDate = referenceCalendar!!.time
        referenceCalendar!!.set(DATE, referenceCalendar!!.get(DATE) + 1)
        val stopDate = referenceCalendar!!.time
        val startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name, startDate)
        val secondStartEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name, secondStartDate)
        val stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name, stopDate)
        val usagesByDay = Arrays.asList<CloudbreakUsage>(cloudbreakUsage)
        `when`(intervalUsageGenerator!!.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay)
        val stack = ServiceTestUtils.createStack()
        stack.status = Status.AVAILABLE

        //WHEN
        val usageList = underTest!!.generate(Arrays.asList(startEvent, secondStartEvent, stopEvent))

        //THEN
        verify(intervalUsageGenerator).generateUsages(startDate, stopDate, startEvent)
        assertFalse(usageList.isEmpty())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackUsageGeneratorTest::class.java)
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
    }
}