package com.sequenceiq.cloudbreak.service.usages;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

public class DefaultCloudbreakUsagesServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsagesServiceTest.class);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @InjectMocks
    private DefaultCloudbreakUsageGeneratorService usagesGeneratorService;

    @Mock
    private CloudbreakUsageRepository usageRepository;

    @Mock
    private CloudbreakEventRepository eventRepository;

    @Before
    public void setUp() throws Exception {
        usagesGeneratorService = new DefaultCloudbreakUsageGeneratorService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDayDates() throws Exception {

        Date now = new Date();
        String todayStr = dateFormat.format(now);

        LOGGER.info("Now: {}", now);
        Date todayA = dateFormat.parse(todayStr);

        LOGGER.info("Today from str: {}", todayA);

        long diff = now.getTime() - todayA.getTime();
        LOGGER.info("Difference in millis : {}", diff);

        LOGGER.info("Difference in hours: {}", TimeUnit.MILLISECONDS.toHours(diff));


        long endoftheday = todayA.getTime() + TimeUnit.HOURS.toMillis(24) - 1;
        LOGGER.info("EndOfTheDay in millis: {}", endoftheday);
        LOGGER.info("EndOfTheDay date: {}", new Date(endoftheday));

    }

    @Test
    public void testShouldGenerateUsageFromStackAvailableAndStackStopEvents() throws Exception {
        //GIVEN

        Calendar cal = Calendar.getInstance();
        cal.setLenient(true);
        cal.setTime(new Date());
        // yesterday
        cal.roll(Calendar.DAY_OF_MONTH, false);
        Date startDate = cal.getTime();

        // tomorrow
        cal.roll(Calendar.DAY_OF_MONTH, 2);
        Date stopDate = cal.getTime();

        CloudbreakEvent availableEvent = ServiceTestUtils.createEvent(1L, 1L, "AVAILABLE", startDate);
        CloudbreakEvent notAvailableEvent = ServiceTestUtils.createEvent(1L, 1L, "DELETE_IN_PROGRESS", stopDate);
        BDDMockito.given(eventRepository.cloudbreakEvents(1L)).willReturn(Arrays.asList(availableEvent, notAvailableEvent));

        //WHEN
        usagesGeneratorService.generateCloudbreakUsages(1L);


        //THEN


    }
}