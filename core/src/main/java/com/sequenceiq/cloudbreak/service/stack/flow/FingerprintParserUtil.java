package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FingerprintParserUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FingerprintParserUtil.class);

    private static final Pattern[] FINGERPRINT_PATTERNS = {
            Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*ECDSA"),
            Pattern.compile("(?<fingerprint>([a-f0-9]{2}:){15,}[a-f0-9]{2}).*RSA")
    };

    private FingerprintParserUtil() {
    }

    public static Set<String> parseFingerprints(String consoleLog) {
        LOGGER.debug("Received console log: {}", consoleLog);
        Set<String> matchedFingerprints = new HashSet<>();
        String[] lines = consoleLog.split("\n");
        for (String line : lines) {
            for (Pattern pattern : FINGERPRINT_PATTERNS) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    matchedFingerprints.add(m.group("fingerprint"));
                }
            }
        }
        return matchedFingerprints;
    }
}
