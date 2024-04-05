package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

@Component
public class SshSaltPasswordActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSaltPasswordActions.class);

    private static final String PASSWORD_CHANGE_SET_COMMAND_PATTERN = "sudo chage -d \"%s\" saltuser";

    private static final DateTimeFormatter CHAGE_DATE_PATTERN = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final String SALTUSER_SHADOW_LINE_COMMAND = "sudo cat /etc/shadow | grep saltuser";

    private static final String PASSWORD_CHANGE_GET_COMMAND = "sudo chage -l saltuser | grep \"Last password change\" | cut -d \":\" -f2";

    @Inject
    private SshJClient sshJClient;

    public void setPasswordChangeDate(Set<String> ipAddresses, LocalDate date) {
        String command = String.format(PASSWORD_CHANGE_SET_COMMAND_PATTERN, CHAGE_DATE_PATTERN.format(date));
        LOGGER.info("Setting password expiry date on nodes {} to {} with command {}", ipAddresses, date, command);
        Map<String, Pair<Integer, String>> results = sshJClient.executeCommands(ipAddresses, command);
        LOGGER.debug("Password expiry set results: {}", results);
        if (results.values().stream().anyMatch(result -> result.getLeft() != 0)) {
            throw new TestFailException("Failed to set saltuser password change date");
        }
    }

    public String getShadowLine(Set<String> ipAddresses) {
        LOGGER.info("Getting saltuser shadow line on nodes {}", ipAddresses);
        Map<String, Pair<Integer, String>> results = sshJClient.executeCommands(ipAddresses, SALTUSER_SHADOW_LINE_COMMAND);
        LOGGER.debug("Saltuser shadow line result: {}", results);
        return results.values().stream()
                .filter(result -> result.getKey() == 0)
                .map(Pair::getValue)
                .findFirst()
                .orElseThrow(() -> new TestFailException("Failed to get saltuser shadow file line on nodes " + ipAddresses));
    }

    public LocalDate getPasswordChangeDate(Set<String> ipAddresses) {
        LOGGER.info("Getting saltuser password expiry date on nodes {}", ipAddresses);
        Map<String, Pair<Integer, String>> results = sshJClient.executeCommands(ipAddresses, PASSWORD_CHANGE_GET_COMMAND);
        LOGGER.debug("Saltuser password expiry date results: {}", results);
        return results.values().stream()
                .filter(result -> result.getKey() == 0)
                .map(result -> LocalDate.parse(result.getValue().trim(), CHAGE_DATE_PATTERN))
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new TestFailException("Failed to get password expiry date on nodes " + ipAddresses));
    }
}
