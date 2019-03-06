package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetStackParamValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class GetStackParamValidationHandler implements CloudPlatformEventHandler<GetStackParamValidationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetStackParamValidationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetStackParamValidationRequest> type() {
        return GetStackParamValidationRequest.class;
    }

    @Override
    public void accept(Event<GetStackParamValidationRequest> getStackParametersRequestEvent) {
        LOGGER.debug("Received event: {}", getStackParametersRequestEvent);
        GetStackParamValidationRequest request = getStackParametersRequestEvent.getData();
        try {
            CloudConnector<Object> aDefault = cloudPlatformConnectors.getDefault(request.getCloudContext().getPlatform());
            GetStackParamValidationResult getStackParamValidationResult = new GetStackParamValidationResult(request,
                    aDefault.parameters().additionalStackParameters());
            request.getResult().onNext(getStackParamValidationResult);
            LOGGER.debug("Query platform stack parameters finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetStackParamValidationResult(e.getMessage(), e, request));
        }
    }
}
