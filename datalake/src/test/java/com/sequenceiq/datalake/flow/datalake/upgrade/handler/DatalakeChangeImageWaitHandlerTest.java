package com.sequenceiq.datalake.flow.datalake.upgrade.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeChangeImageWaitRequest;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeUpgradeFailedEvent;
import com.sequenceiq.datalake.flow.datalake.upgrade.event.DatalakeVmReplaceEvent;
import com.sequenceiq.datalake.service.sdx.SdxUpgradeService;

@ExtendWith(MockitoExtension.class)
class DatalakeChangeImageWaitHandlerTest {

    private static final String IMAGE_ID = "imageId";

    private static final long SDX_ID = 1L;

    @Mock
    private SdxUpgradeService upgradeService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private DatalakeChangeImageWaitHandler underTest;

    @Test
    void whenDatalakeImageisSameDlReplacedEventShouldBeEmitted() {
        UpgradeOptionV4Response upgradeOptionV4Response = new UpgradeOptionV4Response();
        ImageInfoV4Response upgradeImageInfoV4Response = new ImageInfoV4Response();
        upgradeImageInfoV4Response.setImageId(IMAGE_ID);
        upgradeOptionV4Response.setUpgrade(upgradeImageInfoV4Response);
        Event<DatalakeChangeImageWaitRequest> event = new Event<>(new DatalakeChangeImageWaitRequest(1L, "userCrn", upgradeOptionV4Response));
        when(upgradeService.getImageId(SDX_ID)).thenReturn(IMAGE_ID);
        underTest.accept(event);
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event resultEvent = sentEvent.getValue();
        assertEquals("DatalakeVmReplaceEvent", eventNotified);
        assertEquals(DatalakeVmReplaceEvent.class, resultEvent.getData().getClass());
        assertEquals(SDX_ID, ((Payload) resultEvent.getData()).getResourceId());
    }

    @Test
    void whenDatalakeImageisDifferentDlReplaceFailedEventShouldBeEmitted() {
        UpgradeOptionV4Response upgradeOptionV4Response = new UpgradeOptionV4Response();
        ImageInfoV4Response upgradeImageInfoV4Response = new ImageInfoV4Response();
        upgradeImageInfoV4Response.setImageId(IMAGE_ID);
        upgradeOptionV4Response.setUpgrade(upgradeImageInfoV4Response);
        Event<DatalakeChangeImageWaitRequest> event = new Event<>(new DatalakeChangeImageWaitRequest(1L, "userCrn", upgradeOptionV4Response));
        when(upgradeService.getImageId(SDX_ID)).thenReturn("NotTheSameImageId");
        underTest.accept(event);
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event resultEvent = sentEvent.getValue();
        assertEquals("DatalakeUpgradeFailedEvent", eventNotified);
        assertEquals(DatalakeUpgradeFailedEvent.class, resultEvent.getData().getClass());
        assertEquals(SDX_ID, ((Payload) resultEvent.getData()).getResourceId());
    }

    @Test
    void whenUserBreakExceptionHappensFailedEventShouldBeEmitted() {
        UpgradeOptionV4Response upgradeOptionV4Response = new UpgradeOptionV4Response();
        ImageInfoV4Response upgradeImageInfoV4Response = new ImageInfoV4Response();
        upgradeImageInfoV4Response.setImageId(IMAGE_ID);
        upgradeOptionV4Response.setUpgrade(upgradeImageInfoV4Response);
        Event<DatalakeChangeImageWaitRequest> event = new Event<>(new DatalakeChangeImageWaitRequest(1L, "userCrn", upgradeOptionV4Response));
        doThrow(new UserBreakException("Polling exited.")).when(upgradeService).waitCloudbreakFlow(eq(SDX_ID), any(), eq("Change image"));
        underTest.accept(event);
        final ArgumentCaptor<String> eventSelector = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Event> sentEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eventSelector.capture(), sentEvent.capture());
        String eventNotified = eventSelector.getValue();
        Event resultEvent = sentEvent.getValue();
        assertEquals("DatalakeUpgradeFailedEvent", eventNotified);
        assertEquals(DatalakeUpgradeFailedEvent.class, resultEvent.getData().getClass());
        assertEquals(SDX_ID, ((Payload) resultEvent.getData()).getResourceId());
    }
}