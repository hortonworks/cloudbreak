package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;

import reactor.bus.Event;

@Component
public class FileSystemValidationHandler implements CloudPlatformEventHandler<FileSystemValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemValidationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<FileSystemValidationRequest> type() {
        return FileSystemValidationRequest.class;
    }

    @Override
    public void accept(Event<FileSystemValidationRequest> requestEvent) {
        LOGGER.debug("Received event: {}", requestEvent);
        FileSystemValidationRequest request = requestEvent.getData();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(request.getCloudContext().getPlatformVariant());
            connector.setup().validateFileSystem(request.getCredential(), request.getSpiFileSystem());
            request.getResult().onNext(new FileSystemValidationResult(request));
        } catch (Exception e) {
            request.getResult().onNext(new FileSystemValidationResult(e.getMessage(), e, request));
        }
    }
}
