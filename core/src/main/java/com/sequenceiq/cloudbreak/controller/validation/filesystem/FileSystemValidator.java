package com.sequenceiq.cloudbreak.controller.validation.filesystem;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.FileSystemValidationV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

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

    @Inject
    private CloudStorageConverter cloudStorageConverter;

    public void validate(String platform, CloudCredential cloudCredential, CloudStorageBase cloudStorageRequest,
            String userId, Long workspaceId) {
        if (cloudStorageRequest == null) {
            return;
        }
        LOGGER.info("Sending fileSystemRequest to {} to validate the file system", platform);
        CloudContext cloudContext = new CloudContext(null, null, null, platform, userId, workspaceId);
        SpiFileSystem spiFileSystem = cloudStorageConverter.requestToSpiFileSystem(cloudStorageRequest);
        FileSystemValidationRequest request = new FileSystemValidationRequest(spiFileSystem, cloudCredential, cloudContext);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            FileSystemValidationResult result = request.await();
            LOGGER.info("File system validation result: {}", result);
            Exception exception = result.getErrorDetails();
            if (exception != null) {
                throw new BadRequestException(result.getStatusReason(), exception);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while sending the file system validation request", e);
            throw new OperationException(e);
        }
    }

    @Measure(FileSystemValidator.class)
    public void validateFileSystem(String platform, CloudCredential cloudCredential, FileSystemValidationV4Request fileSystemValidationV4Request,
            String userId, Long workspaceId) {
        if (fileSystemValidationV4Request == null) {
            return;
        }
        LOGGER.info("Sending fileSystemRequest to {} to validate the file system", platform);
        CloudContext cloudContext = new CloudContext(null, null, null, platform, userId, workspaceId);
        SpiFileSystem spiFileSystem = converterUtil.convert(fileSystemValidationV4Request, SpiFileSystem.class);
        FileSystemValidationRequest request = new FileSystemValidationRequest(spiFileSystem, cloudCredential, cloudContext);
        eventBus.notify(request.selector(), eventFactory.createEvent(request));
        try {
            FileSystemValidationResult result = request.await();
            LOGGER.info("File system validation result: {}", result);
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
