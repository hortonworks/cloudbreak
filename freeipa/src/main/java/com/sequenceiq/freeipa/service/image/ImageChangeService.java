package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageChangeRequest;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class ImageChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageChangeService.class);

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    public FlowIdentifier changeImage(String accountId, ImageChangeRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        ImageChangeEvent imageChangeEvent = new ImageChangeEvent(IMAGE_CHANGE_EVENT.event(), stack.getId(), request.getImageSettings());
        LOGGER.info("Triggering image change flow with event: {}", imageChangeEvent);
        return flowManager.notify(imageChangeEvent.selector(), imageChangeEvent);
    }
}
