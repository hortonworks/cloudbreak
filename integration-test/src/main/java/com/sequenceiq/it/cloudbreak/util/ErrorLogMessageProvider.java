package com.sequenceiq.it.cloudbreak.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.responses.AuditEventV4Response;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.it.cloudbreak.context.Clue;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class ErrorLogMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorLogMessageProvider.class);

    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public Throwable getException(Map<String, Exception> exceptionsDuringTest) {
        return exceptionsDuringTest.values().stream()
                .filter(exception -> exception.getCause() instanceof TestFailException || exception instanceof TestFailException)
                .findFirst()
                .orElse(null);
    }

    public String getMessage(Map<String, Exception> exceptionsDuringTest, List<Clue> clues) {
        StringBuilder messageBuilder = new StringBuilder("All Exceptions that occurred during the test are logged after this message.")
                .append(System.lineSeparator());
        exceptionsDuringTest.forEach((msg, ex) -> {
            LOGGER.error("Exception during test: " + msg, ex);
            messageBuilder.append(msg).append(": ")
                    .append(ResponseUtil.getErrorMessage(ex))
                    .append(System.lineSeparator())
                    .append(getStackTrace(ex))
                    .append(System.lineSeparator());
        });
        addCluesToMessage(messageBuilder, clues);
        return messageBuilder.toString().replace("%", "%%");
    }

    private String getStackTrace(Exception ex) {
        StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    void addCluesToMessage(StringBuilder builder, List<Clue> clues) {
        try {
            if (clues.stream().anyMatch(Clue::isHasSpotTermination)) {
                String spotTerminatedNames = clues.stream()
                        .filter(Clue::isHasSpotTermination)
                        .map(Clue::getName)
                        .collect(Collectors.joining(", "));

                LOGGER.warn("There were spot terminations in the following resources: {}", spotTerminatedNames);
                builder.append("There were spot terminations in the following resources: ")
                        .append(spotTerminatedNames)
                        .append(System.lineSeparator());
            }

            List<Clue> failureOrderedClues = clues.stream()
                    .sorted((clue1, clue2) -> {
                        boolean clue1HasFailed = hashFailedStructuredEvent(clue1);
                        boolean clue2HasFiled = hashFailedStructuredEvent(clue2);
                        if (clue1HasFailed == clue2HasFiled) {
                            return 0;
                        } else if (clue1HasFailed) {
                            return -1;
                        } else {
                            return 1;
                        }
                    })
                    .collect(Collectors.toList());
            clues = failureOrderedClues;

            builder.append("You can see structured events and audit events about the resources created during test.").append(System.lineSeparator());
            clues.forEach(clue -> {
                appendLine(builder, "");
                appendLine(builder, clue.getName() + " - " + clue.getCrn());
                appendLine(builder, "Cluster logs: ");
                if (StringUtils.contains(clue.getStorageUrl(), "blob.core.windows.net")) {
                    appendLine(builder, "1. Sign in with Azure CLI on your machine: " +
                            "az login --service-principal -u <app-id> -p <password-or-cert> --tenant <tenant>");
                    appendLine(builder, "2. download cluster logs: " +
                            "az storage copy -s " + clue.getStorageUrl() + " -d /path/to/destionation_dir --recursive");
                } else {
                    appendLine(builder, "<a href=\"" + clue.getStorageUrl() + "\" target=\"_blank\">" + clue.getStorageUrl() + "</a>");
                }
                appendLine(builder, "Kibana query: ");
                appendLine(builder, "<a href=\"" + clue.getSearchUrl() + "\" target=\"_blank\">" + clue.getSearchUrl() + "</a>");
                if (clue.getAuditEvents() != null && CollectionUtils.isNotEmpty(clue.getAuditEvents().getResponses())) {
                    appendLine(builder, "Audit events:");
                    appendLine(builder, formatAuditEvents(clue.getAuditEvents().getResponses()));
                }

                if (CollectionUtils.isNotEmpty(clue.getCdpStructuredEvents())) {
                    appendLine(builder, "Structured events:");
                    appendLine(builder, formatStructuredEvents(clue.getCdpStructuredEvents()));
                }
            });

            builder.append("Responses:")
                    .append(System.lineSeparator());
            clues.forEach(clue -> builder.append(clue.getName())
                    .append(" response: ")
                    .append(convertToString(clue.getResponse()))
                    .append(System.lineSeparator()));
            builder.append("All audit events:")
                    .append(System.lineSeparator());
            clues.forEach(clue -> builder.append(clue.getName() + " - " + clue.getCrn())
                    .append(" audit events: ")
                    .append(convertToString(clue.getAuditEvents()))
                    .append(System.lineSeparator()));
        } catch (Exception e) {
            LOGGER.warn("Exception occurred during processing clues. Clues: {}.", convertToString(clues), e);
        }
    }

    private String formatStructuredEvents(List<CDPStructuredEvent> events) {
        return events
                .stream()
                .map(event -> {
                    if (event instanceof CDPStructuredFlowEvent) {
                        CDPStructuredFlowEvent<?> flowEvent = (CDPStructuredFlowEvent<?>) event;
                        return String.format("- [FlowEvent(flowId=%s)] %s - %s %s",
                                flowEvent.getFlow().getFlowId(),
                                flowEvent.getStatus(),
                                flowEvent.getStatusReason(),
                                doubleDecodeBase64(flowEvent.getException()));
                    } else if (event instanceof CDPStructuredNotificationEvent) {
                        return String.format("- [Notification] %s - %s",
                                event.getStatus(),
                                event.getStatusReason());
                    } else {
                        return String.format("- %s - %s",
                                event.getStatus(),
                                event.getStatusReason());
                    }
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatAuditEvents(Collection<AuditEventV4Response> events) {
        return events
                .stream()
                .filter(auditEventV4Response -> auditEventV4Response.getStructuredEvent() instanceof StructuredNotificationEvent)
                .map(auditEventV4Response -> (StructuredNotificationEvent) auditEventV4Response.getStructuredEvent())
                .map(structuredNotificationEvent -> {
                    NotificationDetails notificationDetails = structuredNotificationEvent.getNotificationDetails();
                    return "- " + notificationDetails.getNotificationType() + " - " + notificationDetails.getNotification();
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private void appendLine(StringBuilder builder, String content) {
        builder.append(content).append(System.lineSeparator());
    }

    private String convertToString(Object o) {
        try {
            return OBJECT_WRITER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "[ERROR] Failed to process json from " + o;
        }
    }

    private static String doubleDecodeBase64(String value) {
        if (value == null) {
            return "";
        }
        try {
            return " Exception: " + decode(decode(value));
        } catch (Exception e) {
            return " WARN: Couldn't decode base64 encoded exception.";
        }
    }

    private boolean hashFailedStructuredEvent(Clue clue) {
        if (CollectionUtils.isEmpty(clue.getCdpStructuredEvents())) {
            return false;
        }
        return clue.getCdpStructuredEvents().stream().anyMatch(event -> {
            if (event instanceof CDPStructuredFlowEvent) {
                CDPStructuredFlowEvent flowEvent = (CDPStructuredFlowEvent<?>) event;
                return StringUtils.isNotBlank(flowEvent.getException());
            }
            return false;
        });
    }

    private static String decode(String value) {
        return new String(Base64Util.decodeAsByteArray(value), StandardCharsets.UTF_8);
    }
}
