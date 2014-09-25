package com.sequenceiq.cloudbreak.service.usages;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

public class DefaultCloudbreakUsagesServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsagesServiceTest.class);
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @InjectMocks
    private DefaultCloudbreakUsageGeneratorService usagesGeneratorService;

    @Mock
    private CloudbreakUsageRepository usageRepository;

    @Mock
    private CloudbreakEventRepository eventRepository;

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
    }

    @Before
    public void setUp() throws Exception {
        usagesGeneratorService = new DefaultCloudbreakUsageGeneratorService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testShouldGenerateUsageFromStackAvailableAndStackStopEvents() throws Exception {
        //GIVEN
        referenceCalendar.roll(Calendar.DAY_OF_MONTH, false);
        Date startDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.DAY_OF_MONTH, 2);
        Date stopDate = referenceCalendar.getTime();

        CloudbreakEvent availableEvent = ServiceTestUtils.createEvent(1L, 1L, "AVAILABLE", startDate);
        CloudbreakEvent notAvailableEvent = ServiceTestUtils.createEvent(1L, 1L, "DELETE_IN_PROGRESS", stopDate);
        BDDMockito.given(eventRepository.cloudbreakEvents(1L)).willReturn(Arrays.asList(availableEvent, notAvailableEvent));

        //WHEN
        List<CloudbreakUsage> usageList = usagesGeneratorService.generateCloudbreakUsages(1L);

        //THEN
        Assert.assertTrue("The number of the generated usages is not the expected", usageList.size() == 3);
        Assert.assertEquals("The start day is wrong", DATE_FORMAT.format(startDate), DATE_FORMAT.format(usageList.get(0).getDay()));
        Assert.assertEquals("The stop day is wrong", DATE_FORMAT.format(stopDate), DATE_FORMAT.format(usageList.get(2).getDay()));
        Assert.assertEquals("The span day is wrong", "2014-09-24", DATE_FORMAT.format(usageList.get(1).getDay()));
    }

    @Test
    public void shouldGenerateASingleOneHourUsageWhenStackStartedAndStoppedInOneHour() {
        // GIVEN
        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date startDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date stopDate = referenceCalendar.getTime();

        CloudbreakEvent availableEvent = ServiceTestUtils.createEvent(1L, 1L, "AVAILABLE", startDate);
        CloudbreakEvent notAvailableEvent = ServiceTestUtils.createEvent(1L, 1L, "DELETE_IN_PROGRESS", stopDate);
        BDDMockito.given(eventRepository.cloudbreakEvents(1L)).willReturn(Arrays.asList(availableEvent, notAvailableEvent));

        // WHEN
        List<CloudbreakUsage> usageList = usagesGeneratorService.generateCloudbreakUsages(1L);

        // THEN
        Assert.assertTrue("The number of the generated usages is not the expected", usageList.size() == 1);
        Assert.assertEquals("The start day is wrong", "1", usageList.get(0).getRunningHours());
    }

    @Test
    public void shouldGenerateTwoUsagesForTwoStacksWhenStacksStartedAndStoppedTheSameDay() {
        // GIVEN
        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date startDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date stopDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date startDate2 = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date stopDate2 = referenceCalendar.getTime();

        CloudbreakEvent availableEvent = ServiceTestUtils.createEvent(1L, 1L, "AVAILABLE", startDate);
        CloudbreakEvent notAvailableEvent = ServiceTestUtils.createEvent(1L, 1L, "DELETE_IN_PROGRESS", stopDate);

        CloudbreakEvent availableEvent2 = ServiceTestUtils.createEvent(2L, 1L, "AVAILABLE", startDate2);
        CloudbreakEvent notAvailableEvent2 = ServiceTestUtils.createEvent(2L, 1L, "DELETE_IN_PROGRESS", stopDate2);

        BDDMockito.given(eventRepository.cloudbreakEvents(1L))
                .willReturn(Arrays.asList(availableEvent, notAvailableEvent, availableEvent2, notAvailableEvent2));

        // WHEN
        List<CloudbreakUsage> usageList = usagesGeneratorService.generateCloudbreakUsages(1L);

        // THEN
        Assert.assertTrue("The number of the generated usages is not the expected", usageList.size() == 2);
        Assert.assertEquals("The start day is wrong", "1", usageList.get(0).getRunningHours());
        Assert.assertEquals("The start day is wrong", "1", usageList.get(1).getRunningHours());
    }

    @Test
    public void shouldSingleUsageForStackWithMultipleAvailableStatusEventsOnTheSameDay() {
        // GIVEN
        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date startDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date updateDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date availableDate = referenceCalendar.getTime();

        referenceCalendar.roll(Calendar.HOUR_OF_DAY, 1);
        Date stopDate = referenceCalendar.getTime();

        CloudbreakEvent availableEvent = ServiceTestUtils.createEvent(1L, 1L, "AVAILABLE", startDate);
        CloudbreakEvent updateEvent = ServiceTestUtils.createEvent(1L, 1L, "UPDATE_IN_PROGRESS", updateDate);
        CloudbreakEvent availableEvent2 = ServiceTestUtils.createEvent(1L, 1L, "AVAILABLE", availableDate);
        CloudbreakEvent notAvailableEvent2 = ServiceTestUtils.createEvent(1L, 1L, "DELETE_IN_PROGRESS", stopDate);

        BDDMockito.given(eventRepository.cloudbreakEvents(1L))
                .willReturn(Arrays.asList(availableEvent, updateEvent, availableEvent2, notAvailableEvent2));

        // WHEN
        List<CloudbreakUsage> usageList = usagesGeneratorService.generateCloudbreakUsages(1L);

        // THEN
        Assert.assertTrue("The number of the generated usages is not the expected", usageList.size() == 1);
        Assert.assertEquals("The start day is wrong", "3", usageList.get(0).getRunningHours());
    }
}