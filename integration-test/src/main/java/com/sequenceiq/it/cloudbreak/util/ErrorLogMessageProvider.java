package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.sequenceiq.it.cloudbreak.context.Clue;

@Component
public class ErrorLogMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorLogMessageProvider.class);

    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public String getMessage(Map<String, Exception> exceptionsDuringTest, List<Clue> clues) {
        StringBuilder messageBuilder = new StringBuilder("All Exceptions that occurred during the test are logged after this message")
                .append(System.lineSeparator());
        exceptionsDuringTest.forEach((msg, ex) -> {
            LOGGER.error("Exception during test: " + msg, ex);
            messageBuilder.append(msg).append(": ")
                    .append(ResponseUtil.getErrorMessage(ex))
                    .append(System.lineSeparator());
        });
        addCluesToMessage(messageBuilder, clues);
        return messageBuilder.toString().replace("%", "%%");
    }

    private void addCluesToMessage(StringBuilder builder, List<Clue> clues) {
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
        builder.append("Responses:")
                .append(System.lineSeparator());
        clues.forEach(clue -> builder.append(clue.getName())
                .append(" response: ")
                .append(convertToString(clue.getResponse()))
                .append(System.lineSeparator()));
        builder.append("All audit events:")
                .append(System.lineSeparator());
        clues.forEach(clue -> builder.append(clue.getName())
                .append(" audit events: ")
                .append(convertToString(clue.getAuditEvents()))
                .append(System.lineSeparator()));
    }

    private String convertToString(Object o) {
        try {
            return OBJECT_WRITER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "[ERROR] Failed to process json from " + o;
        }
    }
}
