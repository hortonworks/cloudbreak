package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_IMAGE_CHANGE_STARTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@ExtendWith(MockitoExtension.class)
class ImageChangeActionsTest {

    @InjectMocks
    private ImageChangeActions underTest;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageConverter imageConverter;

    @Mock
    private ImageFallbackService imageFallbackService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Stack stack;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    private StackContext context;

    private ImageChangeEvent payload;

    @BeforeEach
    void setUp() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("stack")
                .withCrn("crn:cdp:freeipa:test")
                .withPlatform(Platform.platform("AWS"))
                .withVariant("variant")
                .withLocation(location(region("us-west-1"), availabilityZone("us-west-1a")))
                .withUserName("user")
                .withAccountId("acc")
                .build();
        context = new StackContext(new FlowParameters("flow", "user-crn"), stack, cloudContext, cloudCredential, cloudStack);

        ImageSettingsRequest request = new ImageSettingsRequest();
        request.setId("img-1");
        payload = new ImageChangeEvent(1L, request);
    }

    @Test
    void prepareImageSendsStartedNotification() throws Exception {
        Map<Object, Object> variables = new HashMap<>();
        ImageEntity imageEntity = new ImageEntity();
        Image image = org.mockito.Mockito.mock(Image.class);
        doReturn(imageEntity).when(imageService).getByStack(stack);
        doReturn(false).when(imageFallbackService).imageFallbackPermitted(imageEntity, stack);
        doReturn(cloudStack).when(cloudStackConverter).convert(stack);
        doReturn(image).when(imageConverter).convert(imageEntity);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), payload)).when(reactorEventFactory).createEvent(any(), any());

        AbstractImageChangeAction<ImageChangeEvent> action = (AbstractImageChangeAction<ImageChangeEvent>) underTest.prepareImage();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, cloudStackConverter, StackToCloudStackConverter.class);
        ReflectionTestUtils.setField(action, null, imageService, ImageService.class);
        ReflectionTestUtils.setField(action, null, imageConverter, ImageConverter.class);
        ReflectionTestUtils.setField(action, null, imageFallbackService, ImageFallbackService.class);

        new AbstractActionTestSupport<>(action).doExecute(context, payload, variables);

        verify(eventSenderService).sendEventAndNotification(stack, "user-crn", FREEIPA_IMAGE_CHANGE_STARTED, List.of("img-1"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, null, stackUpdater, StackUpdater.class);
    }
}

