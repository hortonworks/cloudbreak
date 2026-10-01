package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;

@Component
public class SshSudoCommandActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSudoCommandActions.class);

    @Inject
    private SshJClientActions sshJClientActions;

    public void executeCommand(Collection<String> ipAddresses, String user, String password, String... sudoCommands) {
        String commandsAsSingleCommand = Arrays.stream(sudoCommands)
                .map(command -> getSudoCommand(password, command))
                .collect(Collectors.joining(" && "));
        ipAddresses.forEach(ipAddress -> {
            SshCommandOutcome outcome = sshJClientActions.executeCommand(ipAddress, user, password, null, commandsAsSingleCommand);
            if (!outcome.executed()) {
                throw new TestFailException(String.format("SSH %s on '%s' while executing command '%s'. %s",
                        outcome.result(), ipAddress, commandsAsSingleCommand, outcome.exceptionMessage().orElse(null)), outcome.throwable());
            } else {
                if (!outcome.returnCodeSuccess()) {
                    LOGGER.error(String.format("Unexpected exit code [%s] for command '%s'. Output: %s",
                            outcome.returnCode(), commandsAsSingleCommand, outcome.commandOutput()));
                    throw new TestFailException(String.format("sudo command failed on '%s' for user '%s'. ", ipAddress, user));
                } else {
                    LOGGER.info(String.format("Expected exit code [%s] for command '%s'. Output: %s",
                            outcome.returnCode(), commandsAsSingleCommand, outcome.commandOutput()));
                }
            }
        });
    }

    public List<SshCommandOutcome> executeCommandWithoutThrowing(Collection<String> ipAddresses, String... sudoCommands) {
        return executeCommandWithoutThrowing(ipAddresses, Arrays.asList(sudoCommands));
    }

    public List<SshCommandOutcome> executeCommandWithoutThrowing(Collection<String> ipAddresses, Collection<String> sudoCommands) {
        String commandsAsSingleCommand = sudoCommands.stream()
                .map(command -> getSudoCommand(null, command))
                .collect(Collectors.joining(" && "));
        return ipAddresses.parallelStream()
                .map(ipAddress -> sshJClientActions.executeCommand(ipAddress, null, null, null, commandsAsSingleCommand))
                .toList();
    }

    private String getSudoCommand(String password, String sudoCommand) {
        // Double escape double quotes, since the whole command will be wrapped in double quotes;
        // escape dollar signs to allow for command substitution
        String escapedCommand = sudoCommand
                .replace("\"", "\\\"")
                .replace("$", "\\$");

        // Wrap the escaped command in bash -c "<COMMAND>" so that the whole command is executed with sudo
        String sudoWithPassword = "echo \"" + password + "\" | sudo -S bash -c \"" + escapedCommand + "\"";
        return StringUtils.isEmpty(password)
                ? "sudo bash -c \"" + escapedCommand + "\""
                : sudoWithPassword;
    }
}
