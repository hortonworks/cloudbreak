package com.sequenceiq.cloudbreak.common.anonymizer;

import java.util.regex.Pattern;

public class AnonymizerUtil {

    public static final String REPLACEMENT = "$1****";

    private static final ReplacePattern[] PATTERNS = {
            //common PW
            new ReplacePattern("(?i)("
                    + "password=|password\":\"|password:|password |"
                    + "pass=|pass\":\"|pass:|pass |"
                    + "key=|key\":\"|key:|key |"
                    + "credential=|credential\":\"|credential:|credential "
                    + ")([^\\s'\"]*)", REPLACEMENT),
            //WASB
            new ReplacePattern("(?i)(\\.blob\\.core\\.windows\\.net\":\")([^\\s'\"])*", REPLACEMENT),
            //CM
            new ReplacePattern("(\"name\":\\s*\"[^\"]*password\",\\s*\"value\":\\s*\")[^\\s'\"]*", REPLACEMENT)
    };

    private AnonymizerUtil() {
    }

    public static String anonymize(String content) {
        String ret = content;
        if (ret != null) {
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



