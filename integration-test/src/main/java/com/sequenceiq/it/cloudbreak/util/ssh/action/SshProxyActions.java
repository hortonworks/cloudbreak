package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;

@Component
public class SshProxyActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshProxyActions.class);

    private static final String PROXY_SETTINGS_COMMAND = "sudo cat /etc/cdp/proxy.env";

    @Inject
    private SshJClientActions sshJClientActions;

    public Optional<String> getProxySettings(Collection<String> ipAddresses) {
        LOGGER.info("Getting proxy settings on instances {}", ipAddresses);
        SshCommandOutcomes results = sshJClientActions.executeSshCommandOnHosts(ipAddresses, PROXY_SETTINGS_COMMAND).requireAllExecuted();
        LOGGER.info("Proxy settings output on instances: {}", results);
        Set<String> proxySettings = results.stream()
                .filter(SshCommandOutcome::returnCodeSuccess)
                .map(SshCommandOutcome::commandOutput)
                .collect(Collectors.toSet());
        if (proxySettings.size() > 1) {
            String message = String.format("Found different proxy settings on instances: %s", proxySettings);
            LOGGER.error(message);
            throw new TestFailException(message);
        }
        return proxySettings.stream().findFirst();
    }
}
