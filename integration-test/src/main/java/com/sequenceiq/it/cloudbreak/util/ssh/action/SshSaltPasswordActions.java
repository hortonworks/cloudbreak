package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;

@Component
public class SshSaltPasswordActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSaltPasswordActions.class);

    private static final String PASSWORD_CHANGE_SET_COMMAND_PATTERN = "sudo chage -d \"%s\" saltuser";

    private static final DateTimeFormatter CHAGE_DATE_PATTERN = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private static final String SALTUSER_SHADOW_LINE_COMMAND = "sudo cat /etc/shadow | grep saltuser";

    private static final String PASSWORD_CHANGE_GET_COMMAND = "sudo chage -l saltuser | grep \"Last password change\" | cut -d \":\" -f2";

    @Inject
    private SshJClientActions sshJClientActions;

    public void setPasswordChangeDate(Collection<String> ipAddresses, LocalDate date) {
        String command = String.format(PASSWORD_CHANGE_SET_COMMAND_PATTERN, CHAGE_DATE_PATTERN.format(date));
        LOGGER.info("Setting password expiry date on nodes {} to {} with command {}", ipAddresses, date, command);
        SshCommandOutcomes results = sshJClientActions.executeSshCommandOnHosts(ipAddresses, command);
        LOGGER.debug("Password expiry set results: {}", results);
        results.requireAllSucceeded("Setting saltuser password change date");
    }

    public String getShadowLine(Collection<String> ipAddresses) {
        LOGGER.info("Getting saltuser shadow line on nodes {}", ipAddresses);
        SshCommandOutcomes results = sshJClientActions.executeSshCommandOnHosts(ipAddresses, SALTUSER_SHADOW_LINE_COMMAND);
        LOGGER.debug("Saltuser shadow line result: {}", results);
        return results.requireAllExecuted().stream()
                .filter(SshCommandOutcome::returnCodeSuccess)
                .map(SshCommandOutcome::commandOutput)
                .findFirst()
                .orElseThrow(() -> new TestFailException("Failed to get saltuser shadow file line on nodes " + ipAddresses));
    }

    public LocalDate getPasswordChangeDate(Collection<String> ipAddresses) {
        LOGGER.info("Getting saltuser password expiry date on nodes {}", ipAddresses);
        SshCommandOutcomes results = sshJClientActions.executeSshCommandOnHosts(ipAddresses, PASSWORD_CHANGE_GET_COMMAND);
        LOGGER.debug("Saltuser password expiry date results: {}", results);
        return results.requireAllExecuted().stream()
                .filter(SshCommandOutcome::returnCodeSuccess)
                .map(result -> LocalDate.parse(result.commandOutput().trim(), CHAGE_DATE_PATTERN))
                .min(LocalDate::compareTo)
                .orElseThrow(() -> new TestFailException("Failed to get password expiry date on nodes " + ipAddresses));
    }
}
