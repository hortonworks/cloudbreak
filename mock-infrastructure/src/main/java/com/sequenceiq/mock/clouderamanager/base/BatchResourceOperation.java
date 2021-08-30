package com.sequenceiq.mock.clouderamanager.base;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.clouderamanager.base.batchapi.BatchApiHandler;
import com.sequenceiq.mock.swagger.model.ApiBatchRequest;
import com.sequenceiq.mock.swagger.model.ApiBatchRequestElement;
import com.sequenceiq.mock.swagger.model.ApiBatchResponse;
import com.sequenceiq.mock.swagger.model.ApiBatchResponseElement;

@Service
public class BatchResourceOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchResourceOperation.class);

    private final ResponseCreatorComponent responseCreatorComponent;

    private final List<BatchApiHandler> batchApiHandlers;

    public BatchResourceOperation(ResponseCreatorComponent responseCreatorComponent, List<BatchApiHandler> batchApiHandlers) {
        this.responseCreatorComponent = responseCreatorComponent;
        this.batchApiHandlers = batchApiHandlers;
    }

    @PostConstruct
    void init() {
        String handlers = batchApiHandlers.stream()
                .map(batchApiHandler -> String.format("%s: %s", batchApiHandler.getClass().getName(), batchApiHandler.getDescription()))
                .collect(Collectors.joining(",\n"));
        LOGGER.debug("Registered handlers:\n" + handlers);
    }

    public ResponseEntity<ApiBatchResponse> execute(String mockUuid, @Valid ApiBatchRequest body) {
        LOGGER.debug("Processing request. MockUuid: [{}]. Request: {}", mockUuid, body);
        ApiBatchResponse apiBatchResponse = new ApiBatchResponse();
        if (body == null || body.getItems() == null) {
            apiBatchResponse.success(false);
        } else {
            AtomicBoolean success = new AtomicBoolean(true);
            List<ApiBatchResponseElement> apiBatchResponseElements = body.getItems().stream()
                    .map(apiBatchRequestElement -> {
                        ApiBatchResponseElement apiBatchResponseElement = processApiBatchRequestElement(mockUuid, apiBatchRequestElement);
                        success.set(isSuccessResponse(apiBatchResponseElement) && success.get());
                        return apiBatchResponseElement;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            apiBatchResponse.success(success.get());
            apiBatchResponse.items(apiBatchResponseElements);
        }
        LOGGER.debug("Processing request completed. MockUuid: [{}]. Response: {}", mockUuid, apiBatchResponse);
        return responseCreatorComponent.exec(apiBatchResponse);
    }

    private ApiBatchResponseElement processApiBatchRequestElement(String mockUuid, ApiBatchRequestElement apiBatchRequestElement) {
        for (BatchApiHandler batchApiHandler : batchApiHandlers) {
            if (batchApiHandler.canProcess(apiBatchRequestElement)) {
                try {
                    return batchApiHandler.process(mockUuid, apiBatchRequestElement);
                } catch (RuntimeException e) {
                    LOGGER.error(String.format("Error while processing request, returning failed response. MockUuid: [%s]. Request: %s", mockUuid,
                            apiBatchRequestElement), e);
                    return getFailureApiBatchResponseElement();
                }
            }
        }
        LOGGER.warn("No handler found to process request, returning failed response. MockUuid: [{}]. Request: {}", mockUuid, apiBatchRequestElement);
        return getFailureApiBatchResponseElement();
    }

    private ApiBatchResponseElement getFailureApiBatchResponseElement() {
        return new ApiBatchResponseElement()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    private boolean isSuccessResponse(ApiBatchResponseElement apiBatchResponseElement) {
        HttpStatus httpStatus = Optional.ofNullable(apiBatchResponseElement)
                .map(ApiBatchResponseElement::getStatusCode)
                .map(HttpStatus::resolve)
                .orElse(null);
        if (httpStatus != null) {
            boolean success = httpStatus.is2xxSuccessful() || httpStatus.is3xxRedirection();
            LOGGER.debug("Mapping response status: [{}]. Success: {}", httpStatus, success);
            return success;
        } else {
            LOGGER.warn("Invalid status code for response, assuming failure. Response: " + apiBatchResponseElement);
            return false;
        }
    }

}
