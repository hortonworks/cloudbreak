package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateResult;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.eventbus.Event;

@Component
public class TemplateHandler implements CloudPlatformEventHandler<GetPlatformTemplateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetPlatformTemplateRequest> type() {
        return GetPlatformTemplateRequest.class;
    }

    @Override
    public void accept(Event<GetPlatformTemplateRequest> platformTemplateRequestEvent) {
        LOGGER.debug("Received event: {}", platformTemplateRequestEvent);
        GetPlatformTemplateRequest request = platformTemplateRequestEvent.getData();
        String template = null;
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            if (connector != null) {
                template = connector.resources().getStackTemplate();
            }
        } catch (TemplatingNotSupportedException ignored) {
        }
        GetPlatformTemplateResult getPlatformTemplateResult = new GetPlatformTemplateResult(request.getResourceId(), template);
        request.getResult().onNext(getPlatformTemplateResult);
        LOGGER.debug("Get template finished.");
    }
}
