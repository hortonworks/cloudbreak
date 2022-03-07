package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class StackPatchUsageReporterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPatchUsageReporterService.class);

    @Inject
    private UsageReporter usageReporter;

    public void reportUsage(StackPatch stackPatch) {
        UsageProto.CDPStackPatchEventType.Value eventType = convertStatusToEventType(stackPatch.getStatus());
        sendUsageReport(stackPatch.getStack(), stackPatch.getType(), eventType, stackPatch.getStatusReason());
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

    private void sendUsageReport(Stack stack, StackPatchType stackPatchType, UsageProto.CDPStackPatchEventType.Value eventType, String optionalMessage) {
        String resourceCrn = Optional.ofNullable(stack).map(Stack::getResourceCrn).orElse("");
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
