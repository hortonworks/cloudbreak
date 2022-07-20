package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateEvent.STACK_IMAGE_UPDATE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_UPDATE_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_IMAGE_UPDATE_STARTED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.UpdateImageRequest;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.ImageUpdateEvent;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Configuration
public class StackImageUpdateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackImageUpdateActions.class);

    @Bean(name = "CHECK_IMAGE_VERSIONS_STATE")
    public AbstractStackImageUpdateAction<?> checkImageVersion() {
        return new AbstractStackImageUpdateAction<>(StackImageUpdateTriggerEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackImageUpdateTriggerEvent payload, Map<Object, Object> variables) {
                getFlowMessageService().fireEventAndLog(context.getStack().getId(), Status.UPDATE_IN_PROGRESS.name(), STACK_IMAGE_UPDATE_STARTED);
                if (!getStackImageUpdateService().isCbVersionOk(context.getStack())) {
                    throw new OperationException("Stack must be created at least with Cloudbreak version [" + StackImageUpdateService.MIN_VERSION + ']');
                }
                StatedImage newImage = getStackImageUpdateService().getNewImageIfVersionsMatch(context.getStack(), payload.getNewImageId(),
                        payload.getImageCatalogName(), payload.getImageCatalogUrl());
                sendEvent(context, new ImageUpdateEvent(StackImageUpdateEvent.CHECK_IMAGE_VERESIONS_FINISHED_EVENT.event(),
                        context.getStack().getId(), newImage));
            }
        };
    }

    @Bean(name = "CHECK_PACKAGE_VERSIONS_STATE")
    public AbstractStackImageUpdateAction<?> checkPackageVersions() {
        return new AbstractStackImageUpdateAction<>(ImageUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, ImageUpdateEvent payload, Map<Object, Object> variables) {
                CheckResult checkResult = getStackImageUpdateService().checkPackageVersions(context.getStack(), payload.getImage());
                if (checkResult.getStatus() == EventStatus.FAILED) {
                    LOGGER.info("Check package versions failed: {}", checkResult);
                }
                sendEvent(context, new ImageUpdateEvent(StackImageUpdateEvent.CHECK_PACKAGE_VERSIONS_FINISHED_EVENT.event(),
                        context.getStack().getId(), payload.getImage()));
            }
        };
    }

    @Bean(name = "UPDATE_IMAGE_STATE")
    public AbstractStackImageUpdateAction<?> updateImage() {
        return new AbstractStackImageUpdateAction<>(ImageUpdateEvent.class) {
            @Override
            protected void doExecute(StackContext context, ImageUpdateEvent payload, Map<Object, Object> variables) {
                try {
                    variables.put(ORIGINAL_IMAGE, getImageService().getImage(context.getStack().getId()));
                } catch (CloudbreakImageNotFoundException e) {
                    LOGGER.debug("Image not found", e);
                    throw new CloudbreakServiceException(e.getMessage(), e);
                }
                getStackImageService().storeNewImageComponent(context.getStack(), payload.getImage());
                sendEvent(context, new StackEvent(StackImageUpdateEvent.UPDATE_IMAGE_FINESHED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "IMAGE_PREPARE_STATE")
    public AbstractStackImageUpdateAction<?> prepareImageAction() {
        return new AbstractStackImageUpdateAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                getStackCreationService().prepareImage(context.getStack().getId(), variables);
                try {
                    Image image = getImageService().getImage(context.getCloudContext().getId());
                    PrepareImageRequest<Selectable> request =
                            new PrepareImageRequest<>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(), image);
                    sendEvent(context, request);
                } catch (CloudbreakImageNotFoundException e) {
                    throw new CloudbreakServiceException(e);
                }
            }
        };
    }

    @Bean(name = "SET_IMAGE_STATE")
    public AbstractStackImageUpdateAction<?> setImageAction() {
        return new AbstractStackImageUpdateAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Collection<Resource> resources = getResourceService().getAllByStackId(context.getStack().getId());
                List<CloudResource> cloudResources =
                        resources.stream().map(resource -> getResourceToCloudResourceConverter().convert(resource)).collect(Collectors.toList());
                UpdateImageRequest<Selectable> request =
                        new UpdateImageRequest<>(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(), cloudResources);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "STACK_IMAGE_UPDATE_FINISHED")
    public AbstractStackImageUpdateAction<?> finishAction() {
        return new AbstractStackImageUpdateAction<>(CloudPlatformResult.class) {
            @Override
            protected void doExecute(StackContext context, CloudPlatformResult payload, Map<Object, Object> variables) {
                getFlowMessageService().fireEventAndLog(context.getStack().getId(), Status.AVAILABLE.name(), STACK_IMAGE_UPDATE_FINISHED);
                getStackUpdater().updateStackStatus(context.getStack().getId(), DetailedStackStatus.AVAILABLE);
                sendEvent(context, new StackEvent(STACK_IMAGE_UPDATE_FINISHED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @Bean(name = "STACK_IMAGE_UPDATE_FAILED_STATE")
    public AbstractStackFailureAction<StackImageUpdateState, StackImageUpdateEvent> handleImageUpdateFailure() {
        return new AbstractStackFailureAction<>() {
            @Inject
            private CloudbreakFlowMessageService flowMessageService;

            @Inject
            private StackUpdater stackUpdater;

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Error during Stack image update flow:", payload.getException());
                String errorMessage = payload.getException().getMessage();
                flowMessageService.fireEventAndLog(context.getStackId(), Status.UPDATE_FAILED.name(), STACK_IMAGE_UPDATE_FAILED, errorMessage);
                stackUpdater.updateStackStatus(context.getStackId(), DetailedStackStatus.STACK_IMAGE_UPDATE_FAILED, errorMessage);
                sendEvent(context, new StackEvent(StackImageUpdateEvent.STACK_IMAGE_UPDATE_FAILE_HANDLED_EVENT.event(),
                        context.getStackId()));
            }
        };
    }
}
