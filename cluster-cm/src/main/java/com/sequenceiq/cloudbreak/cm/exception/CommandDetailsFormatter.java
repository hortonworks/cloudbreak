package com.sequenceiq.cloudbreak.cm.exception;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CommandDetailsFormatter {

    private static final int DISPLAY_ONLY_COMMANDS_THRESHOLD = 10;

    private static final int MAX_MESSAGE_LENGTH = 500;

    private static final int MAX_SINGLE_MESSAGE_LENGTH = 50;

    private CommandDetailsFormatter() {
    }

    public static String formatFailedCommands(List<CommandDetails> commandDetails) {
        int messageLengthSum = 0;
        for (CommandDetails commandDetail : commandDetails) {
            messageLengthSum += commandDetail.getReason().length();
        }

        String message;
        if (messageLengthSum > MAX_MESSAGE_LENGTH) {
            if (commandDetails.size() > DISPLAY_ONLY_COMMANDS_THRESHOLD) {
                message = '[' + format(commandDetails, cmd -> cmd.getName() + "(id=" + cmd.getId() + ')') + ']';
            } else {
                message = format(commandDetails, cmd -> cmd.getName() + "(id=" + cmd.getId() + "): " + cut(cmd.getReason()));
            }
        } else {
            message = format(commandDetails, cmd -> cmd.getName() + "(id=" + cmd.getId() + "): " + cmd.getReason());
        }
        return "Please find more details on Cloudera Manager UI. Failed command(s): " + message;
    }

    private static String format(List<CommandDetails> commandDetails, Function<CommandDetails, String> formatter) {
        return commandDetails
                .stream()
                .map(formatter)
                .collect(Collectors.joining(" "));
    }

    private static String cut(String message) {
        if (message.length() > MAX_SINGLE_MESSAGE_LENGTH) {
            return message.substring(0, MAX_SINGLE_MESSAGE_LENGTH) + "...";
        } else {
            return message;
        }
    }
}
