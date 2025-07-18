package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshSudoCommandActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSudoCommandActions.class);

    @Inject
    private SshJClient sshJClient;

    public void executeCommand(Collection<String> ipAddresses, String user, String password, String... sudoCommands) {
        String commandsAsSingleCommand = Arrays.stream(sudoCommands)
                .map(command -> getSudoCommand(password, command))
                .collect(Collectors.joining(" && "));
        ipAddresses.forEach(ipAddress -> {
            Pair<Integer, String> result = executeCommand(ipAddress, user, password, commandsAsSingleCommand);
            if (result.getKey() != 0) {
                LOGGER.error("Unexpected exit code [" + result.getKey() + "] for command '" +
                        commandsAsSingleCommand + "'. Output: " + result.getValue());
                throw new TestFailException("sudo command failed on '" + ipAddress + "' for user '" + user + "'. ");
            } else {
                LOGGER.info("Expected exit code [" + result.getKey() + "] for command '" +
                        commandsAsSingleCommand + "'. Output: " + result.getValue());
            }
        });
    }

    public Map<String, Pair<Integer, String>> executeCommandWithoutThrowing(Collection<String> ipAddresses, String... sudoCommands) {
        return executeCommandWithoutThrowing(ipAddresses, Arrays.asList(sudoCommands));
    }

    public Map<String, Pair<Integer, String>> executeCommandWithoutThrowing(Collection<String> ipAddresses, Collection<String> sudoCommands) {
        String commandsAsSingleCommand = sudoCommands.stream()
                .map(command -> getSudoCommand(null, command))
                .collect(Collectors.joining(" && "));
        return ipAddresses.parallelStream()
                .collect(Collectors.toMap(Function.identity(), ipAddress -> executeCommand(ipAddress, commandsAsSingleCommand)));
    }

    private String getSudoCommand(String password, String sudoCommand) {
        // Double escape double quotes, since the whole command will be wrapped in double quotes;
        // escape dollar signs to allow for command substitution
        String escapedCommand = sudoCommand
                .replace("\"", "\\\"")
                .replace("$", "\\$");

        // Wrap the escaped command in bash -c "<COMMAND>" so that the whole command is executed with sudo
        return StringUtils.isEmpty(password)
                ? "sudo bash -c \"" + escapedCommand + "\""
                : "echo " + password + " | sudo -S bash -c \"" + escapedCommand + "\"";
    }

    private Pair<Integer, String> executeCommand(String instanceIP, String command) {
        return executeCommand(instanceIP, null, null, command);
    }

    private Pair<Integer, String> executeCommand(String instanceIP, String user, String password, String command) {
        try (SSHClient sshClient = sshJClient.createSshClient(instanceIP, user, password, null)) {
            return sshJClient.execute(sshClient, command);
        } catch (Exception e) {
            throw new TestFailException(" SSH fail on [" + instanceIP + "] while executing command [" + command + "].", e);
        }
    }
}
