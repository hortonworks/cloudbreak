package com.sequenceiq.cloudbreak.logger;

public class ReplaceUtil {

    private static ReplacePattern[] patterns = {new ReplacePattern("(?i)(?<=password=|password\":\"|pass:)(?=\\S*)[^\\s'\"]*", "****")};

    private ReplaceUtil() {
    }

    public static String anonymize(String content) {
        String ret = content;
        if (ret != null) {
            for (ReplacePattern pattern : patterns) {
                ret = ret.replaceAll(pattern.regexp, pattern.replacement);
            }
        }
        return ret;
    }

    static class ReplacePattern {

        private final String regexp;

        private final String replacement;

        ReplacePattern(String regexp, String replacement) {
            this.regexp = regexp;
            this.replacement = replacement;
        }
    }

}



