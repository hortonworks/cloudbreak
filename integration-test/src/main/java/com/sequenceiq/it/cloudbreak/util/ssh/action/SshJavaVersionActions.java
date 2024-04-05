package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

@Component
public class SshJavaVersionActions  {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshJavaVersionActions.class);

    private static final String JAVA_MAJOR_VERSION_COMMAND = "java -version 2>&1 | grep -oP \"version [^0-9]?(1\\.)?\\K\\d+\"";

    @Inject
    private SshJClient sshJClient;

    public Map<String, Integer> getJavaMajorVersions(Set<String> ipAddresses) {
        return ipAddresses.stream().collect(Collectors.toMap(Function.identity(), ipAddress -> {
            LOGGER.debug("Lookup default java version from '{}'", ipAddress);
            Pair<Integer, String> result = executeCommand(ipAddress);
            if (result.getKey() != 0) {
                throw new TestFailException(format("Java version lookup failed on '%s' by unexpected exit code [%d]. Output: %s",
                        ipAddress, result.getKey(), result.getValue()));
            } else {
                LOGGER.info("Expected exit code [{}}]. Output: {}}", result.getKey(), result.getValue());
            }
            try {
                return parseInt(result.getValue().trim());
            } catch (NumberFormatException expcetion) {
                throw new TestFailException(format("Failed to parse '%s' as an integer", result.getValue()));
            }
        }));
    }

    private Pair<Integer, String> executeCommand(String instanceIP) {
        try (SSHClient sshClient = sshJClient.createSshClient(instanceIP, null, null, null)) {
            return sshJClient.execute(sshClient, JAVA_MAJOR_VERSION_COMMAND);
        } catch (Exception e) {
            throw new TestFailException(format("SSH fail on [%s] while looking for java version.", instanceIP), e);
        }
    }
}
