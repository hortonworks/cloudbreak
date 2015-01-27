package com.sequenceiq.cloudbreak.service.usages;

import static java.util.Calendar.DATE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

public class StackUsageGeneratorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackUsageGeneratorTest.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @InjectMocks
    private StackUsageGenerator underTest;
    @Mock
    private CloudbreakEventRepository eventRepository;
    @Mock
    private IntervalStackUsageGenerator intervalUsageGenerator;
    @Mock
    private StackRepository stackRepository;
    @Mock
    private CloudbreakUsage cloudbreakUsage;

    private Calendar referenceCalendar;

    @Before
    public void before() {
        String referenceDateStr = "2014-09-24";
        try {
            referenceCalendar = Calendar.getInstance();
            referenceCalendar.setTime(DATE_FORMAT.parse(referenceDateStr));
        } catch (ParseException e) {
            LOGGER.error("invalid reference date str: {}, ex: {}", referenceDateStr, e);
        }
        underTest = new StackUsageGenerator();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateShouldEmptyLIstWhenNoEventExists() throws Exception {

        List<CloudbreakUsage> usageList = underTest.generate(new ArrayList<CloudbreakEvent>());

        assertTrue(usageList.isEmpty());
    }

    @Test
    public void testGenerateShouldCreateExactUsageWhenStartAndStopEventsExist() throws Exception {
        //GIVEN
        Date startDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        Date stopDate = referenceCalendar.getTime();
        CloudbreakEvent startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name(), startDate);
        CloudbreakEvent stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name(), stopDate);
        Map<String, CloudbreakUsage> usagesByDay = new HashMap<>();
        usagesByDay.put(DATE_FORMAT.format(startDate), cloudbreakUsage);
        when(intervalUsageGenerator.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.AVAILABLE);
        when(stackRepository.findById(1L)).thenReturn(stack);

        //WHEN
        List<CloudbreakUsage> usageList = underTest.generate(Arrays.asList(startEvent, stopEvent));

        //THEN
        verify(intervalUsageGenerator).generateUsages(startDate, stopDate, startEvent);
        verify(stackRepository).findById(1L);
        verify(stackRepository, never()).delete(stack);
        assertFalse(usageList.isEmpty());
    }

    @Test
    public void testGenerateShouldDeleteStackWhenTheStackStateIsDeleteCompleted() throws Exception {
        //GIVEN
        Date startDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        Date stopDate = referenceCalendar.getTime();
        CloudbreakEvent startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name(), startDate);
        CloudbreakEvent stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name(), stopDate);
        Map<String, CloudbreakUsage> usagesByDay = new HashMap<>();
        usagesByDay.put(DATE_FORMAT.format(startDate), cloudbreakUsage);
        when(intervalUsageGenerator.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.DELETE_COMPLETED);
        when(stackRepository.findById(1L)).thenReturn(stack);

        //WHEN
        List<CloudbreakUsage> usageList = underTest.generate(Arrays.asList(startEvent, stopEvent));

        //THEN
        verify(intervalUsageGenerator).generateUsages(startDate, stopDate, startEvent);
        verify(stackRepository).findById(1L);
        verify(stackRepository, times(1)).delete(stack);
        assertFalse(usageList.isEmpty());
    }

    @Test
    public void testGenerateShouldCreateExactUsageWhenStopEventDoesNotExist() throws Exception {
        //GIVEN
        Date startDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        CloudbreakEvent startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name(), startDate);
        Map<String, CloudbreakUsage> usagesByDay = new HashMap<>();
        usagesByDay.put(DATE_FORMAT.format(startDate), cloudbreakUsage);
        when(intervalUsageGenerator.generateUsages(any(Date.class), any(Date.class), any(CloudbreakEvent.class))).thenReturn(usagesByDay);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.AVAILABLE);
        when(stackRepository.findById(1L)).thenReturn(stack);

        //WHEN
        List<CloudbreakUsage> usageList = underTest.generate(Arrays.asList(startEvent));

        //THEN
        verify(stackRepository, never()).findById(1L);
        verify(stackRepository, never()).delete(stack);
        assertFalse(usageList.isEmpty());
    }

    @Test
    public void testGenerateShouldCreateNewBillingStartEventWhenStopEventDoesNotExist() throws Exception {
        //GIVEN
        Date startDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        CloudbreakEvent startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name(), startDate);
        Map<String, CloudbreakUsage> usagesByDay = new HashMap<>();
        usagesByDay.put(DATE_FORMAT.format(startDate), cloudbreakUsage);
        when(intervalUsageGenerator.generateUsages(any(Date.class), any(Date.class), any(CloudbreakEvent.class))).thenReturn(usagesByDay);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.AVAILABLE);
        when(stackRepository.findById(1L)).thenReturn(stack);

        //WHEN
        List<CloudbreakUsage> usageList = underTest.generate(Arrays.asList(startEvent));

        //THEN
        verify(stackRepository, never()).findById(1L);
        verify(stackRepository, never()).delete(stack);
        verify(eventRepository, times(1)).save(any(CloudbreakEvent.class));
        assertFalse(usageList.isEmpty());
    }

    @Test
    public void testGenerateShouldReturnEmptyListWhenStartEventDoesNotExist() throws Exception {
        //GIVEN
        Date startDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        Date stopDate = referenceCalendar.getTime();
        CloudbreakEvent startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name(), startDate);
        CloudbreakEvent stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name(), stopDate);
        Map<String, CloudbreakUsage> usagesByDay = new HashMap<>();
        usagesByDay.put(DATE_FORMAT.format(startDate), cloudbreakUsage);
        when(intervalUsageGenerator.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.AVAILABLE);
        when(stackRepository.findById(1L)).thenReturn(stack);

        //WHEN
        List<CloudbreakUsage> usageList = underTest.generate(Arrays.asList(startEvent, stopEvent));

        //THEN
        assertTrue(usageList.isEmpty());
    }

    @Test
    public void testGenerateShouldConsiderTheFirstStartWhenMoreStartEventsExist() throws Exception {
        //GIVEN
        Date startDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        Date secondStartDate = referenceCalendar.getTime();
        referenceCalendar.set(DATE, referenceCalendar.get(DATE) + 1);
        Date stopDate = referenceCalendar.getTime();
        CloudbreakEvent startEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name(), startDate);
        CloudbreakEvent secondStartEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STARTED.name(), secondStartDate);
        CloudbreakEvent stopEvent = ServiceTestUtils.createEvent(1L, 1, BillingStatus.BILLING_STOPPED.name(), stopDate);
        Map<String, CloudbreakUsage> usagesByDay = new HashMap<>();
        usagesByDay.put(DATE_FORMAT.format(startDate), cloudbreakUsage);
        when(intervalUsageGenerator.generateUsages(startDate, stopDate, startEvent)).thenReturn(usagesByDay);
        Stack stack = ServiceTestUtils.createStack();
        stack.setStatus(Status.AVAILABLE);
        when(stackRepository.findById(1L)).thenReturn(stack);

        //WHEN
        List<CloudbreakUsage> usageList = underTest.generate(Arrays.asList(startEvent, secondStartEvent, stopEvent));

        //THEN
        verify(intervalUsageGenerator).generateUsages(startDate, stopDate, startEvent);
        verify(stackRepository).findById(1L);
        verify(stackRepository, never()).delete(stack);
        assertFalse(usageList.isEmpty());
    }
}