package com.sequenceiq.it.cloudbreak.util;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class ErrorLogMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorLogMessageProvider.class);

    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public Throwable getException(Map<String, Exception> exceptionsDuringTest) {
        return exceptionsDuringTest.values().stream()
                .filter(exception -> exception.getCause() instanceof TestFailException)
                .findFirst()
                .orElse(null);
    }

    public String getMessage(Map<String, Exception> exceptionsDuringTest, List<Clue> clues) {
        StringBuilder messageBuilder = new StringBuilder("All Exceptions that occurred during the test are logged after this message")
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
        } catch (Exception e) {
            LOGGER.warn("Exception occurred during processing clues. Clues: {}.", convertToString(clues), e);
        }
    }

    private String convertToString(Object o) {
        try {
            return OBJECT_WRITER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "[ERROR] Failed to process json from " + o;
        }
    }
}
