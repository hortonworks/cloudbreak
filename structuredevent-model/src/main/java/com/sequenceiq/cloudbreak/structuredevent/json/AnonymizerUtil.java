package com.sequenceiq.cloudbreak.structuredevent.json;

import java.util.regex.Pattern;

public class AnonymizerUtil {

    public static final String REPLACEMENT = "****";

    private static final ReplacePattern[] PATTERNS = {
            //common PW
            new ReplacePattern("(?i)(?<="
                    + "password=|password\":\"|password:|password |"
                    + "pass=|pass\":\"|pass:|pass |"
                    + "key=|key\":\"|key:|key |"
                    + "credential=|credential\":\"|credential:|credential "
                    + ")(?=\\S*)[^\\s'\"]*", REPLACEMENT),
            //WASB
            new ReplacePattern("(?i)(?<=\\.blob\\.core\\.windows\\.net\":\")(?=\\S*)[^\\s'\"]*", REPLACEMENT),
            //CM
            new ReplacePattern("(?<=\"name\":\\s{0,100}\"[^\"]{0,100}password\",\\s{0,100}\"value\":\\s{0,100}\")[^\\s'\"]*", REPLACEMENT)
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



