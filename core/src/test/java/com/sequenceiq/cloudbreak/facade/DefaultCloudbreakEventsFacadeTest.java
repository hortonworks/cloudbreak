package com.sequenceiq.cloudbreak.facade;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCloudbreakEventsFacadeTest {

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private DefaultCloudbreakEventsFacade underTest;

    @Test
    public void findUsagesForParametersConvertUsagesToJson() {
        List<CloudbreakEvent> cloudbreakEvents = TestUtil.generateAzureCloudbreakEvents(10);
        when(cloudbreakEventService.cloudbreakEvents(anyString(), anyLong())).thenReturn(cloudbreakEvents);
        when(conversionService.convert(anyObject(), any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(new ArrayList<CloudbreakEventsJson>());

        underTest.retrieveEvents("owner", new Date().getTime());

        verify(cloudbreakEventService, times(1)).cloudbreakEvents(anyString(), anyLong());
        verify(conversionService, times(1)).convert(anyObject(), any(TypeDescriptor.class), any(TypeDescriptor.class));
    }

}