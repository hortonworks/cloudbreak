package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;

import java.util.Collection;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;

@Component
public class SshJavaVersionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshJavaVersionActions.class);

    private static final String JAVA_MAJOR_VERSION_COMMAND = "java -version 2>&1 | grep -oP \"version [^0-9]?(1\\.)?\\K\\d+\"";

    @Inject
    private SshJClientActions sshJClientActions;

    public Map<String, Integer> getJavaMajorVersions(Collection<String> ipAddresses) {
        LOGGER.debug("Lookup default java version from '{}'", ipAddresses);
        return sshJClientActions.executeSshCommandOnHosts(ipAddresses, JAVA_MAJOR_VERSION_COMMAND)
                .requireAllSucceeded("Java version lookup")
                .toMapByInstanceIp(this::parseJavaMajorVersion);
    }

    private Integer parseJavaMajorVersion(SshCommandOutcome result) {
        LOGGER.info("Expected exit code [{}]. Output: {}", result.returnCode(), result.commandOutput());
        try {
            return parseInt(result.commandOutput().trim());
        } catch (NumberFormatException expcetion) {
            throw new TestFailException(format("Failed to parse '%s' as an integer", result.commandOutput()));
        }
    }
}
