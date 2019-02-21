package com.sequenceiq.cloudbreak.controller.validation.filesystem;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.FileSystemValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.OperationException;

import reactor.bus.EventBus;

@Component
public class FileSystemValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemValidator.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private ConverterUtil converterUtil;

    public void validateCloudStorage(String platform, CloudCredential cloudCredential, CloudStorageV4Request cloudStorageV4Request,
            String userId, Long workspaceId) {
        if (cloudStorageV4Request == null) {
            return;
        }
        LOGGER.debug("Sending fileSystemRequest to {} to validate the file system", platform);
        CloudContext cloudContext = new CloudContext(null, null, platform, userId, workspaceId);
        SpiFileSystem spiFileSystem = converterUtil.convert(cloudStorageV4Request, SpiFileSystem.class);
        FileSystemValidationRequest request = new FileSystemValidationRequest(spiFileSystem, cloudCredential, cloudContext);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            FileSystemValidationResult result = request.await();
            LOGGER.debug("File system validation result: {}", result);
            Exception exception = result.getErrorDetails();
            if (exception != null) {
                throw new BadRequestException(result.getStatusReason(), exception);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while sending the file system validation request", e);
            throw new OperationException(e);
        }
    }

    public void validateFileSystem(String platform, CloudCredential cloudCredential, FileSystemValidationV4Request fileSystemValidationV4Request,
            String userId, Long workspaceId) {
        if (fileSystemValidationV4Request == null) {
            return;
        }
        LOGGER.debug("Sending fileSystemRequest to {} to validate the file system", platform);
        CloudContext cloudContext = new CloudContext(null, null, platform, userId, workspaceId);
        SpiFileSystem spiFileSystem = converterUtil.convert(fileSystemValidationV4Request, SpiFileSystem.class);
        FileSystemValidationRequest request = new FileSystemValidationRequest(spiFileSystem, cloudCredential, cloudContext);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            FileSystemValidationResult result = request.await();
            LOGGER.debug("File system validation result: {}", result);
            Exception exception = result.getErrorDetails();
            if (exception != null) {
                throw new BadRequestException(result.getStatusReason(), exception);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while sending the file system validation request", e);
            throw new OperationException(e);
        }
    }

}
