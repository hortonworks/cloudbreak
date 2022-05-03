package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshSudoCommandActions extends SshJClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSudoCommandActions.class);

    public void executeCommand(Set<String> ipAddresses, String user, String password, String... sudoCommands) {
        ipAddresses.stream().forEach(ipAddress -> {
            String commandsAsSingleCommand = Arrays.stream(sudoCommands).map(command -> getSudoCommand(password, command))
                    .collect(Collectors.joining(" && "));
            Pair<Integer, String> result = executeCommand(ipAddress, user, password, commandsAsSingleCommand);
            if (result.getKey().intValue() != 0) {
                LOGGER.error("Unexpected exit code [" + result.getKey().intValue() + "] for command '" +
                        commandsAsSingleCommand + "'. Output: " + result.getValue());
                throw new TestFailException("sudo command failed on '" + ipAddress + "' for user '" + user + "'. ");
            } else {
                LOGGER.info("Expected exit code [" + result.getKey().intValue() + "] for command '" +
                        commandsAsSingleCommand + "'. Output: " + result.getValue());
            }
        });
    }

    private String getSudoCommand(String password, String sudoCommand) {
        return StringUtils.isEmpty(password) ?
                "sudo " + sudoCommand : "echo " + password + " | sudo -S " + sudoCommand;
    }

    private Pair<Integer, String> executeCommand(String instanceIP, String user, String password, String command) {
        try (SSHClient sshClient = createSshClient(instanceIP, user, password, null)) {
            return execute(sshClient, command);
        } catch (Exception e) {
            throw new TestFailException(" SSH fail on [" + instanceIP + "] while executing command [" + command + "].", e);
        }
    }
}
