package com.sequenceiq.stackpatcher.service;

import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.domain.StackPatchResource;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;
import com.sequenceiq.stackpatcher.domain.StackPatchType;

@Service
public class StackPatchUsageReporterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPatchUsageReporterService.class);

    @Inject
    private UsageReporter usageReporter;

    public void reportStatusChange(StackPatchResource resource, StackPatchType type, StackPatchStatus status, String statusReason) {
        UsageProto.CDPStackPatchEventType.Value eventType = convertStatusToEventType(status);
        sendUsageReport(resource, type, eventType, statusReason);
    }

    public void reportUsage(StackPatchResource resource, StackPatchType stackPatchType, UsageProto.CDPStackPatchEventType.Value eventType,
            String optionalMessage) {
        sendUsageReport(resource, stackPatchType, eventType, optionalMessage);
    }

    private UsageProto.CDPStackPatchEventType.Value convertStatusToEventType(StackPatchStatus stackPatchStatus) {
        switch (stackPatchStatus) {
            case AFFECTED:
                return UsageProto.CDPStackPatchEventType.Value.AFFECTED;
            case FIXED:
                return UsageProto.CDPStackPatchEventType.Value.SUCCESS;
            case FAILED:
                return UsageProto.CDPStackPatchEventType.Value.FAILURE;
            default:
                LOGGER.warn("Unrecognized StackPatchStatus {} for CDPStackPatchEventType", stackPatchStatus);
                return UsageProto.CDPStackPatchEventType.Value.UNRECOGNIZED;
        }
    }

    private void sendUsageReport(StackPatchResource resource, StackPatchType stackPatchType, UsageProto.CDPStackPatchEventType.Value eventType,
            String optionalMessage) {
        String resourceCrn = Optional.ofNullable(resource).map(StackPatchResource::getResourceCrn).orElse("");
        String stackPatchTypeName = Objects.requireNonNullElse(stackPatchType, StackPatchType.UNKNOWN).name();
        String message = Objects.requireNonNullElse(optionalMessage, "");
        try {
            Objects.requireNonNull(eventType);
            LOGGER.info("Reporting stack patch event with resource crn {}, stack patch type {}, event type {} and message {}",
                    resourceCrn, stackPatchType, eventType, message);
            UsageProto.CDPStackPatchEvent cdpStackPatchEvent = UsageProto.CDPStackPatchEvent.newBuilder()
                    .setResourceCrn(resourceCrn)
                    .setStackPatchType(stackPatchTypeName)
                    .setEventType(eventType)
                    .setMessage(message)
                    .build();
            usageReporter.cdpStackPatcherEvent(cdpStackPatchEvent);
        } catch (Exception e) {
            LOGGER.error("Failed to report stack patch event with resource crn {}, stack patch type {}, event type {} and message {}",
                    resourceCrn, stackPatchType, eventType, message, e);
        }
    }
}
