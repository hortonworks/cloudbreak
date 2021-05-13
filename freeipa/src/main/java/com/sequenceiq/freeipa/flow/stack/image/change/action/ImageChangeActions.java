package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FINISHED_EVENT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.stack.AbstractStackFailureAction;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureContext;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.ImageChangeState;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Configuration
public class ImageChangeActions {

    public static final String ORIGINAL_IMAGE_REVISION = "ORIGINAL_IMAGE_REVISION";

    public static final String ORIGINAL_IMAGE = "ORIGINAL_IMAGE";

    public static final String IMAGE_ENTITY_ID = "IMAGE_ENTITY_ID";

    public static final String IMAGE_CHANGED_IN_DB = "IMAGE_CHANGED_IN_DB";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageChangeActions.class);

    @Bean(name = "CHANGE_IMAGE_STATE")
    public AbstractImageChangeAction<?> changeImage() {
        return new ImageChangeAction();
    }

    @Bean(name = "PREPARE_IMAGE_STATE")
    public AbstractImageChangeAction<?> prepareImage() {
        return new AbstractImageChangeAction<>(ImageChangeEvent.class) {
            @Inject
            private ImageConverter imageConverter;

            @Inject
            private ImageService imageService;

            @Override
            protected void doExecute(StackContext context, ImageChangeEvent payload, Map<Object, Object> variables) throws Exception {
                CloudStack cloudStack = getCloudStackConverter().convert(context.getStack());
                Image image = imageConverter.convert(imageService.getByStack(context.getStack()));
                PrepareImageRequest<Object> request = new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, image);
                LOGGER.info("Prepare image: {}", image);
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(ImageChangeEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("[PREPARE_IMAGE_STATE] failed", ex);
                return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "SET_IMAGE_ON_PROVIDER_STATE")
    public AbstractImageChangeAction<?> setImageOnProvider() {
        return new AbstractImageChangeAction<>(StackEvent.class) {
            @Inject
            private ResourceService resourceService;

            @Inject
            private ResourceToCloudResourceConverter cloudResourceConverter;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                CloudStack cloudStack = getCloudStackConverter().convert(context.getStack());
                Collection<Resource> resources = resourceService.findAllByStackId(context.getStack().getId());
                List<CloudResource> cloudResources =
                        resources.stream().map(resource -> cloudResourceConverter.convert(resource)).collect(Collectors.toList());
                UpdateImageRequest<Selectable> request =
                        new UpdateImageRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudStack, cloudResources);
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("[SET_IMAGE_ON_PROVIDER_STATE] failed", ex);
                return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex);
            }
        };
    }

    @Bean(name = "IMAGE_CHANGE_FINISHED_STATE")
    public AbstractImageChangeAction<?> finishImageChange() {
        return new AbstractImageChangeAction<>(CloudPlatformResult.class) {
            @Override
            protected void doExecute(StackContext context, CloudPlatformResult payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context, new StackEvent(IMAGE_CHANGE_FINISHED_EVENT.event(), context.getStack().getId()));
            }

            @Override
            protected Object getFailurePayload(CloudPlatformResult payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("[IMAGE_CHANGE_FINISHED_STATE] failed", ex);
                return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<CloudPlatformResult>> payloadConverters) {
                payloadConverters.add(new ImageChangeEventToCloudPlatformResultConverter());
            }
        };
    }

    @Bean(name = "IMAGE_CHANGE_FAILED_STATE")
    public Action<?, ?> handleImageChangeFailure() {
        return new AbstractStackFailureAction<ImageChangeState, ImageChangeEvents>() {
            @Inject
            private ImageService imageService;

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.error("Image change failed", payload.getException());
                if (variables.containsKey(ORIGINAL_IMAGE_REVISION) && variables.containsKey(IMAGE_CHANGED_IN_DB)) {
                    LOGGER.info("Reverting to original image using revision [{}]", variables.get(ORIGINAL_IMAGE_REVISION));
                    imageService.revertImageToRevision((Long) variables.get(IMAGE_ENTITY_ID), (Number) variables.get(ORIGINAL_IMAGE_REVISION));
                } else if (variables.containsKey(ORIGINAL_IMAGE) && variables.containsKey(IMAGE_CHANGED_IN_DB)) {
                    LOGGER.info("Reverting to original image using entity stored in variables");
                    ImageEntity originalImage = (ImageEntity) variables.get(ORIGINAL_IMAGE);
                    imageService.save(originalImage);
                }
                sendEvent(context, new StackEvent(IMAGE_CHANGE_FAILURE_HANDLED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

}
