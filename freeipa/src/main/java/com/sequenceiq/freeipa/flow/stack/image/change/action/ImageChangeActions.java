package com.sequenceiq.freeipa.flow.stack.image.change.action;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_FALLBACK_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_FALLBACK_FINISHED_EVENT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.flow.stack.provision.PrepareImageResultToStackEventConverter;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;
import com.sequenceiq.freeipa.service.image.ImageFallbackService;
import com.sequenceiq.freeipa.service.image.ImageNotFoundException;
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

            @Inject
            private ImageFallbackService imageFallbackService;

            @Override
            protected void doExecute(StackContext context, ImageChangeEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                CloudContext cloudContext = context.getCloudContext();

                ImageEntity imageEntity = imageService.getByStack(stack);
                String regionName = cloudContext.getLocation().getRegion().value();
                String platform = cloudContext.getPlatform().getValue();
                String fallbackImageName = null;
                if (imageFallbackService.imageFallbackPermitted(imageEntity, stack)) {
                    try {
                        com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image imageForStack = imageService.getImageForStack(stack);
                        fallbackImageName = imageService.determineImageNameByRegion(platform, regionName, imageForStack);
                    } catch (ImageNotFoundException e) {
                        LOGGER.warn("Fallback image could not be determined due to exception {}," +
                                " we should continue execution", e.getMessage());
                    }
                }
                CloudStack cloudStack = getCloudStackConverter().convert(stack);
                Image image = imageConverter.convert(imageEntity);
                PrepareImageRequest<Object> request = new PrepareImageRequest<>(cloudContext, context.getCloudCredential(), cloudStack, image,
                        PrepareImageType.EXECUTED_DURING_IMAGE_CHANGE, fallbackImageName);
                LOGGER.info("Prepare image: {}, fallback image:{}", image, fallbackImageName);
                sendEvent(context, request);
            }

            @Override
            protected Object getFailurePayload(ImageChangeEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("[PREPARE_IMAGE_STATE] failed", ex);
                return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
            }
        };
    }

    @Bean(name = "SET_FALLBACK_IMAGE_STATE")
    public AbstractImageChangeAction<?> imageFallbackAction() {
        return new AbstractImageChangeAction<>(StackEvent.class) {

            @Inject
            private ImageFallbackService imageFallbackService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                Long stackId = stack.getId();
                ImageEntity image = stack.getImage();
                if (imageFallbackService.imageFallbackPermitted(image, stack)) {
                    imageFallbackService.performImageFallback(image, stack);
                }
                ImageFallbackSuccess imageFallbackSuccess = new ImageFallbackSuccess(stackId);
                sendEvent(context, IMAGE_FALLBACK_FINISHED_EVENT.event(), imageFallbackSuccess);
            }

            @Override
            protected Object getFailurePayload(StackEvent payload, Optional<StackContext> flowContext, Exception ex) {
                LOGGER.error("[IMAGE_FALLBACK_STATE] failed", ex);
                return new StackFailureEvent(IMAGE_FALLBACK_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<StackEvent>> payloadConverters) {
                payloadConverters.add(new PrepareImageResultToStackEventConverter());
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
                return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
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
                return new StackFailureEvent(IMAGE_CHANGE_FAILED_EVENT.event(), payload.getResourceId(), ex, ERROR);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<CloudPlatformResult>> payloadConverters) {
                payloadConverters.add(new ImageChangeEventToCloudPlatformResultConverter());
            }
        };
    }

    @Bean(name = "IMAGE_CHANGE_FAILED_STATE")
    public Action<?, ?> handleImageChangeFailure() {
        return new ImageChangeFailureHandlerAction();
    }

}