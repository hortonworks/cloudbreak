package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

@Component
public class SshProxyActions extends SshJClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshProxyActions.class);

    private static final String PROXY_SETTINGS_COMMAND = "sudo cat /etc/cdp/proxy.env";

    public Optional<String> getProxySettings(Set<String> ipAddresses) {
        LOGGER.info("Getting proxy settings on instances {}", ipAddresses);
        Map<String, Pair<Integer, String>> results = executeCommands(ipAddresses, PROXY_SETTINGS_COMMAND);
        LOGGER.info("Proxy settings output on instances: {}", results);
        Set<String> proxySettings = results.values().stream()
                .filter(result -> result.getKey() == 0)
                .map(Pair::getValue)
                .collect(Collectors.toSet());
        if (proxySettings.size() > 1) {
            String message = String.format("Found different proxy settings on instances: %s", proxySettings);
            LOGGER.error(message);
            throw new TestFailException(message);
        }
        return proxySettings.stream().findFirst();
    }
}
