package com.sequenceiq.it.cloudbreak.assertion.encryptionprofile;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class EncryptionProfileAssertions {

    // Map of monitoring services and its config file path.
    public static final Map<String, String> MONITORING_SERVICES_CONFIG_PATH = Map.of(
            "blackbox-exporter",
            "/opt/blackbox_exporter/blackbox_exporter-web-config.yml",
            "node-exporter",
            "/opt/node_exporter/node_exporter-web-config.yml",
            "cdp-prometheus",
            "/opt/cdp-prometheus/prometheus-web-config.yml");

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileAssertions.class);

    private static final String CIPHER_SUITES = "cipher_suites";

    // Expected cipher suites in the correct order for monitoring services.
    private static final List<String> DEFAULT_CIPHER_SUITES = List.of(
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"
    );

    public void validateCipherSuitesConfiguration(String serviceName, String configContent, String instanceIp) {
        // Check if config file contains cipher_suites section
        if (!configContent.contains("cipher_suites:")) {
            Log.error(LOGGER, format("%s config file does not contain 'cipher_suites' section at '%s' instance!",
                    serviceName, instanceIp));
            throw new TestFailException(format("%s config file does not contain 'cipher_suites' section at '%s' instance!",
                    serviceName, instanceIp));
        }

        // Extract cipher suites from config content
        List<String> actualCipherSuites = extractCipherSuitesFromConfig(configContent);

        // Validate cipher suites presence and order
        if (actualCipherSuites.size() != DEFAULT_CIPHER_SUITES.size()) {
            Log.error(LOGGER, format("Expected %d cipher suites but found %d at '%s' instance! Expected: %s, Actual: %s",
                    DEFAULT_CIPHER_SUITES.size(), actualCipherSuites.size(), instanceIp, DEFAULT_CIPHER_SUITES, actualCipherSuites));
            throw new TestFailException(format("Incorrect number of cipher suites at '%s' instance!", instanceIp));
        }

        for (int i = 0; i < DEFAULT_CIPHER_SUITES.size(); i++) {
            String expected = DEFAULT_CIPHER_SUITES.get(i);
            String actual = actualCipherSuites.get(i);

            if (!expected.equals(actual)) {
                Log.error(LOGGER, format("Cipher suite order mismatch at position %d at '%s' instance! Expected: '%s', Actual: '%s'",
                        i, instanceIp, expected, actual));
                throw new TestFailException(format("Cipher suite order mismatch at '%s' instance!", instanceIp));
            }
        }
    }

    private List<String> extractCipherSuitesFromConfig(String configContent) {
        List<String> cipherSuites = new ArrayList<>();
        String[] lines = configContent.split("\n");
        boolean inCipherSuitesSection = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.equals(CIPHER_SUITES)) {
                inCipherSuitesSection = true;
                continue;
            }

            if (inCipherSuitesSection) {
                if (trimmedLine.startsWith("- ")) {
                    // Extract cipher suite name (remove "- " prefix)
                    String cipherSuite = trimmedLine.substring(2).trim();
                    cipherSuites.add(cipherSuite);
                } else if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                    // End of cipher_suites section
                    break;
                }
            }
        }
        return cipherSuites;
    }
}
