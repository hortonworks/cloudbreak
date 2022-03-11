package com.sequenceiq.cloudbreak.common.anonymizer;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnonymizerUtil {

    public static final String REPLACEMENT = "$1****";

    private static final Logger LOGGER = LoggerFactory.getLogger(AnonymizerUtil.class);

    private static final ReplacePattern[] PATTERNS = {
            //common PW
            new ReplacePattern("(?i)("
                    + "password:[\"']|password=[\"']|password=|password\":\"|password:|password |"
                    + "credentialJson:[\"']|credentialJson=[\"']|credentialJson=|credentialJson\":\"|credentialJson:|credentialJson |"
                    + "pass:[\"']|pass=[\"']|pass=|pass\":\"|pass:|pass |"
                    + "secret:[\"']|secret=[\"']|secret=|secret\":\"|secret:|secret |"
                    + "secretKey:[\"']|secretKey=[\"']|secretKey=|secretKey\":\"|secretKey:|secretKey |"
                    + "key:[\"']|key=[\"']|key=|key\":\"|key:|key |"
                    + "credential:[\"']|credential=[\"']|credential=|credential\":\"|credential:|credential "
                    + ")([^\\s'\"]*)", REPLACEMENT),
            //WASB
            new ReplacePattern("(?i)(\\.blob\\.core\\.windows\\.net\":\")([^\\s'\"])*", REPLACEMENT),
            //CM
            new ReplacePattern("(\"name\":\\s*\"[^\"]*password\",\\s*\"value\":\\s*\")[^\\s'\"]*", REPLACEMENT),
            //KNOX
            new ReplacePattern("(\"name\":\\s*\"[^\"]*secret\",\\s*\"value\":\\s*\")[^\\s'\"]*", REPLACEMENT),
            //FreeIPA
            new ReplacePattern("(ldapmodify .* -w ')([^\\s'\"]*)", REPLACEMENT),
            new ReplacePattern("(MagBearerToken=)([^\\s'\";,]*)", REPLACEMENT),
            // AZURE DB ARM TEMPLATE
            new ReplacePattern("(\"type\": \"securestring\",\\s*\"defaultValue\" : \\s*\")[^\\s'\"]*", REPLACEMENT)
    };

    private AnonymizerUtil() {
    }

    public static String anonymize(Object object) {
        return object == null ? null : anonymize(object.toString());
    }

    public static String anonymize(String content) {
        String ret = content;
        if (ret != null) {
            LOGGER.trace("Anonymize the content with length: {}", content.length());
            for (ReplacePattern pattern : PATTERNS) {
                ret = pattern.replaceAll(ret);
            }
        }
        return ret;
    }

    static class ReplacePattern {

        private final Pattern pattern;

        private final String replacement;

        ReplacePattern(String regexp, String replacement) {
            pattern = Pattern.compile(regexp);
            this.replacement = replacement;
        }

        public String replaceAll(String input) {
            return pattern.matcher(input).replaceAll(replacement);
        }
    }

}



