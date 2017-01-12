package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformTemplateResult;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

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
        LOGGER.info("Received event: {}", platformTemplateRequestEvent);
        GetPlatformTemplateRequest request = platformTemplateRequestEvent.getData();
        String template;
        try {
            CloudConnector connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            if (connector != null) {
                template = connector.resources().getStackTemplate();
            } else {
                throw new TemplatingDoesNotSupportedException();
            }
        } catch (TemplatingDoesNotSupportedException e) {
            template = null;
        }
        GetPlatformTemplateResult getPlatformTemplateResult = new GetPlatformTemplateResult(request, template);
        request.getResult().onNext(getPlatformTemplateResult);
        LOGGER.info("Get template finished.");
    }
}
