package com.sequenceiq.cloudbreak.cm.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cm.exception.CommandDetails.CommandStatus;

public class CommandDetailsFormatterTest {

    @Test
    public void testReturnsOriginalMessageWhenMessageIsShort() {
        String message = CommandDetailsFormatter.formatFailedCommands(
                List.of(commandDetail(1, "GenerateCredentials", "Failed to generate credential.")));

        assertEquals("Please find more details on Cloudera Manager UI. Failed command(s): GenerateCredentials(id=1): Failed to generate credential.",
                message);
    }

    @Test
    public void testReturnsFailedCommandsWhenSumOfMessagesIsTooLongAndThereAreTooManyFailedCommands() {
        String message = CommandDetailsFormatter.formatFailedCommands(IntStream.range(0, 11)
                .boxed()
                .map(i -> commandDetail(i, "Command" + i, longMessage()))
                .collect(Collectors.toList()));

        assertEquals("Please find more details on Cloudera Manager UI. Failed command(s): " +
                "[Command0(id=0) Command1(id=1) Command2(id=2) Command3(id=3) " +
                "Command4(id=4) Command5(id=5) Command6(id=6) Command7(id=7) " +
                "Command8(id=8) Command9(id=9) Command10(id=10)]", message);
    }

    @Test
    public void testReturnsShortMessagesWhenSumOfMessagesIsTooLong() {
        String message = CommandDetailsFormatter.formatFailedCommands(IntStream.range(0, 10)
                .boxed()
                .map(i -> commandDetail(i, "Command" + i, longMessage()))
                .collect(Collectors.toList()));

        assertEquals("Please find more details on Cloudera Manager UI. Failed command(s): " +
                "Command0(id=0): Failed to do something. Failed to do something. Fa... " +
                "Command1(id=1): Failed to do something. Failed to do something. Fa... " +
                "Command2(id=2): Failed to do something. Failed to do something. Fa... " +
                "Command3(id=3): Failed to do something. Failed to do something. Fa... " +
                "Command4(id=4): Failed to do something. Failed to do something. Fa... " +
                "Command5(id=5): Failed to do something. Failed to do something. Fa... " +
                "Command6(id=6): Failed to do something. Failed to do something. Fa... " +
                "Command7(id=7): Failed to do something. Failed to do something. Fa... " +
                "Command8(id=8): Failed to do something. Failed to do something. Fa... " +
                "Command9(id=9): Failed to do something. Failed to do something. Fa...", message);
    }

    private static String longMessage() {
        return "Failed to do something. ".repeat(50);
    }

    private static CommandDetails commandDetail(int id, String name, String message) {
        return new CommandDetails(new BigDecimal(id),
                name,
                CommandStatus.FAILED,
                Optional.of(message),
                Optional.of("Service"),
                Optional.of("Role"),
                Optional.of("Host"));
    }

}