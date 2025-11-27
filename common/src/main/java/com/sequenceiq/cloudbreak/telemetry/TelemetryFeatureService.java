package com.sequenceiq.cloudbreak.telemetry;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class TelemetryFeatureService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryFeatureService.class);

    private static final List<Pair<String, Versioned>> ECDSA_PACKAGE_VERSION_REQUIREMENTS = List.of(
            Pair.of("cdp-logging-agent", () -> "0.3.3"),
            Pair.of("cdp-request-signer", () -> "0.2.3"),
            Pair.of("cdp-telemetry", () -> "0.4.30"));

    private static final Pair<String, Versioned> MINIFI_MIN_VERSION_REQUIREMENT = Pair.of("cdp-minifi-agent", () -> "1.25.09");

    public boolean isECDSAAccessKeyTypeSupported(Map<String, String> packages) {
        if (packages == null) {
            return false;
        }
        for (Pair<String, Versioned> packageWithVersion : ECDSA_PACKAGE_VERSION_REQUIREMENTS) {
            String packageName = packageWithVersion.getKey();
            Versioned minimumVersion = packageWithVersion.getValue();
            if (!packages.containsKey(packageName)) {
                LOGGER.warn("Image doesn't contain {} package.", packageName);
            } else {
                String packageVersion = packages.get(packageName);
                if (new VersionComparator().compare(() -> packageVersion, minimumVersion) < 0) {
                    LOGGER.info("{} package's version {} is smaller than {}. Therefore ECDSA based access key is not enabled.",
                            packageName, packageVersion, minimumVersion.getVersion());
                    return false;
                }
            }
        }
        LOGGER.info("ECDSA based access key type is enabled by package versions. {}", packages);
        return true;
    }

    public boolean isMinifiLoggingSupported(Map<String, String> packages) {
        if (packages == null) {
            return false;
        }
        String packageName = MINIFI_MIN_VERSION_REQUIREMENT.getKey();
        if (!packages.containsKey(packageName)) {
            LOGGER.warn("Image doesn't contain {} package. Minifi logging is not supported.", packageName);
            return false;
        } else {
            String packageVersion = packages.get(packageName);
            Versioned minimumVersion = MINIFI_MIN_VERSION_REQUIREMENT.getValue();
            if (new VersionComparator().compare(() -> packageVersion, minimumVersion) < 0) {
                LOGGER.info("{} package's version {} is smaller than {}. Minifi logging is not supported.",
                        packageName, packageVersion, minimumVersion.getVersion());
                return false;
            }
        }
        return true;
    }
}
