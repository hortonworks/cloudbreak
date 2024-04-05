package com.sequenceiq.it.cloudbreak.util.ssh.action;

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
public class SshSafeLogicActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshSafeLogicActions.class);

    private static final String LIST_JRE_EXTENSIONS_COMMAND = "sudo ls ${JAVA_HOME}/jre/lib/ext/";

    private static final Set<String> SAFELOGIC_BINARIES = Set.of("bctls.jar", "ccj.jar");

    private static final String LIST_JAVA_SECURITY_PROVIDERS_COMMAND = "${JAVA_HOME}/bin/jrunscript -e " +
            "\"providers = java.security.Security.getProviders(); for (p in providers) print(providers[p])\"";

    private static final String CCJ_VERSION_PREFIX = "CCJ version";

    private static final String GET_MAX_AES_KEY_LENGTH_COMMAND = "${JAVA_HOME}/bin/jrunscript -Dcom.safelogic.cryptocomply.fips.approved_only=true -e " +
            "\"print(javax.crypto.Cipher.getMaxAllowedKeyLength(\\\"AES/CBC/PKCS5Padding\\\"));\"";

    private static final String EXPECTED_MAX_AES_KEY_LENGTH = "2147483647";

    @Inject
    private SshJClient sshJClient;

    public boolean hasSafeLogicBinaries(Set<String> ipAddresses) {
        LOGGER.info("Getting SafeLogic binaries on instances {}", ipAddresses);
        Map<String, Pair<Integer, String>> results = sshJClient.executeCommands(ipAddresses, LIST_JRE_EXTENSIONS_COMMAND);
        LOGGER.info("List JRE extensions output on instances: {}", results);
        Boolean result = null;

        for (String ip : ipAddresses) {
            if (!results.containsKey(ip) || results.get(ip).getKey() != 0) {
                throw new TestFailException("Failed to list SafeLogic binaries on instance " + ip);
            }
            String commandResult = results.get(ip).getValue();
            LOGGER.debug("Found JRE extensions on node {}: {}", ip, commandResult);
            boolean allSafeLogicBinariesInstalled = Set.of(commandResult.split("\\s+")).containsAll(SAFELOGIC_BINARIES);
            if (result != null && result != allSafeLogicBinariesInstalled) {
                throw new TestFailException("SafeLogic binary installation is not matching on all instances");
            }
            result = allSafeLogicBinariesInstalled;
        }

        return Boolean.TRUE.equals(result);
    }

    public boolean hasCryptoComplyForJavaSecurityProvider(Set<String> ipAddresses) {
        LOGGER.info("Getting Java security providers on instances {}", ipAddresses);
        Map<String, Pair<Integer, String>> results = sshJClient.executeCommands(ipAddresses, LIST_JAVA_SECURITY_PROVIDERS_COMMAND);
        LOGGER.info("List Java security providers output on instances: {}", results);
        Boolean result = null;

        for (String ip : ipAddresses) {
            if (!results.containsKey(ip) || results.get(ip).getKey() != 0) {
                throw new TestFailException("Failed to list Java security providers on instance " + ip);
            }
            String commandResult = results.get(ip).getValue();
            LOGGER.debug("Found Java security providers on node {}: {}", ip, commandResult);
            boolean cryptoComplyForJavaConfigured = commandResult.startsWith(CCJ_VERSION_PREFIX);
            if (result != null && result != cryptoComplyForJavaConfigured) {
                throw new TestFailException("CryptoComply configuration is not matching on all instances");
            }
            result = cryptoComplyForJavaConfigured;
        }

        return Boolean.TRUE.equals(result);
    }

    public void validateMaxAESKeyLength(Set<String> ipAddresses) {
        LOGGER.info("Getting max AES key length on instances {}", ipAddresses);
        Map<String, Pair<Integer, String>> results = sshJClient.executeCommands(ipAddresses, GET_MAX_AES_KEY_LENGTH_COMMAND);
        LOGGER.info("Get max AES key length output on instances: {}", results);
        String result = null;

        for (String ip : ipAddresses) {
            if (!results.containsKey(ip) || results.get(ip).getKey() != 0) {
                throw new TestFailException("Failed to get max AES key length on instance " + ip);
            }
            String commandResult = results.get(ip).getValue().trim();
            LOGGER.debug("Max AES key length on node {}: {}", ip, commandResult);
            if (result != null && !result.equals(commandResult)) {
                throw new TestFailException("Max AES key length is not matching on all instances");
            }
            result = commandResult;
        }

        if (!EXPECTED_MAX_AES_KEY_LENGTH.equals(result)) {
            throw new TestFailException(String.format("Max AES key length %s not matching expected value %s", result, EXPECTED_MAX_AES_KEY_LENGTH));
        }
    }
}
