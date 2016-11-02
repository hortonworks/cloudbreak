package com.sequenceiq.cloudbreak.controller.validation.filesystem;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.validation.FileSystemValidationResult;
import com.sequenceiq.cloudbreak.cloud.model.FileSystem;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.FileSystemRequestToFileSystemConverter;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class FileSystemValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemValidator.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private FileSystemRequestToFileSystemConverter converter;

    public void validateFileSystem(String platform, FileSystemRequest fileSystemRequest) {
        if (fileSystemRequest == null) {
            return;
        }
        validateFilesystemRequest(fileSystemRequest);
        LOGGER.debug("Sending fileSystemRequest to {} to validate the file system", platform);
        CloudContext cloudContext = new CloudContext(null, null, platform, null, null, null);
        FileSystem fileSystem = converter.convert(fileSystemRequest);
        FileSystemValidationRequest request = new FileSystemValidationRequest(fileSystem, cloudContext);
        eventBus.notify(request.selector(), Event.wrap(request));
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

    private void validateFilesystemRequest(FileSystemRequest fileSystemRequest) {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        try {
            String json = JsonUtil.writeValueAsString(fileSystemRequest.getProperties());
            Object fsConfig = JsonUtil.readValue(json, fileSystemRequest.getType().getClazz());
            Set<ConstraintViolation<Object>> violations = validator.validate(fsConfig);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        } catch (IOException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

}
