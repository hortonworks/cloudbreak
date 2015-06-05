package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FingerprintParserUtil {

    private FingerprintParserUtil() {
    }

    public static String parseFingerprint(String log) {
        String[] fingerprints = log.split("cb: -----BEGIN SSH HOST KEY FINGERPRINTS-----|cb: -----END SSH HOST KEY FINGERPRINTS-----")[2].split("\n");
        for (String fingerprint : fingerprints) {
            Pattern fingerprintPattern = Pattern.compile("([a-f0-9]{2}:){15,}[a-f0-9]{2}");
            Matcher m = fingerprintPattern.matcher(fingerprint);
            if (m.find()) {
                return m.group(0);
            }
        }
        return null;
    }
}
