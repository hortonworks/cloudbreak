package com.sequenceiq.cloudbreak.cm.exception;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

/**
 * Redacts and bounds a Cloudera Manager role's stderr log before it is surfaced in an exception message or logged.
 * <p>
 * The processing order is deliberate: the input is first bounded to a safe size, then redacted, and only then
 * truncated. Redacting before the final truncation guarantees that a secret marker is never cut away from its value,
 * leaving the value exposed. The initial size bound looks {@value #REDACTION_OVERLAP} characters in front of its cut
 * and moves the cut past any secret that straddles it, as recognised by this class's patterns or by
 * {@code AnonymizerUtil}'s, so it cannot split a marker from its value either.
 * <p>
 * The class is stateless and its own patterns are precompiled and linear in cost. Some {@code AnonymizerUtil} patterns
 * are not linear (e.g. {@code ldapmodify .* -w '}), but {@link #INPUT_CAP} bounds their cost too, which is acceptable
 * on this failure-only path. {@link #sanitize(String)} never throws; on any problem it logs and returns an empty result.
 * <p>
 * This is a security sanitizer, so it deliberately errs on the side of over-masking rather than leaking: any key whose
 * name ends in a secret word (e.g. {@code password}, {@code secret}, {@code token}, or anything ending in {@code key})
 * is masked, and the shared {@code AnonymizerUtil} adds bare-word false positives such as {@code "key store at"}
 * becoming {@code "key **** at"}. A slightly over-masked log is always preferable to one that leaks a credential.
 */
@Component
public class ClouderaManagerRoleLogSanitizer {

    /**
     * Upper bound on the amount of input the regex work is allowed to run over. Anything longer is reduced to its
     * last {@value} characters before redaction.
     */
    static final int INPUT_CAP = 64 * 1024;

    // Extra input in front of the INPUT_CAP cut that is searched for secrets straddling the cut. It is far longer than
    // any secret marker the patterns accept, so a marker whose value reaches past the cut is always seen whole.
    static final int REDACTION_OVERLAP = 1024;

    /**
     * Maximum length of the redacted {@code tail}.
     */
    static final int MAX_TAIL_LENGTH = 4096;

    /**
     * Maximum length of the redacted {@code extract}.
     */
    static final int MAX_EXTRACT_LENGTH = 200;

    // Capping the scanned length keeps the exception regex linear on pathological word-char runs.
    private static final int MAX_EXCEPTION_LINE_SCAN = 2048;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerRoleLogSanitizer.class);

    private static final String MASK = "****";

    private static final String ELLIPSIS = "...";

    // Matching starts at the secret word itself, not at the start of the key name: any key-name text before it is left
    // untouched anyway, and a leading [\w.-]* run would make every position of a long word-char run backtrack.
    // 'key' deliberately also matches accessKey/privateKey/apiKey/secretKey (over-masking is intended).
    private static final String SECRET_KEY_NAME = "(?:password|passwd|secret|credential|token|key)";

    // A secret assignment: the secret key word, an optional closing quote, ':' or '=', and optional surrounding
    // whitespace. The quote may be backslash-escaped, as in JSON embedded in a JSON string: {\"password\":\"x\"}.
    private static final String SECRET_ASSIGNMENT_PREFIX =
            "(?i)(" + SECRET_KEY_NAME + "(?:\\\\?[\"'])?\\s*[:=]\\s*)";

    // Secret values after such a prefix: escaped-double-quoted, double-quoted, single-quoted (all tolerating a missing
    // closing quote, but never running past the end of the line), or bare. Possessive quantifiers keep each match
    // linear. The whole value is masked as one unit. Inside an escaped value, \\\\ (an escaped backslash) and \\\" (an
    // escaped quote) are consumed whole, so neither is mistaken for the closing \".
    private static final Pattern SECRET_VALUE_ESCAPED_DOUBLE_QUOTED = Pattern.compile(
            SECRET_ASSIGNMENT_PREFIX + "\\\\\"(?:\\\\\\\\\\\\\\\\|\\\\\\\\\\\\\"|[^\"\\\\\\r\\n]|\\\\[^\"\\r\\n])*+(?:\\\\\")?");

    private static final Pattern SECRET_VALUE_DOUBLE_QUOTED = Pattern.compile(
            SECRET_ASSIGNMENT_PREFIX + "\"(?:[^\"\\\\\\r\\n]|\\\\.)*+\"?");

    private static final Pattern SECRET_VALUE_SINGLE_QUOTED = Pattern.compile(
            SECRET_ASSIGNMENT_PREFIX + "'(?:[^'\\\\\\r\\n]|\\\\.)*+'?");

    private static final Pattern SECRET_VALUE_BARE = Pattern.compile(
            SECRET_ASSIGNMENT_PREFIX + "(?!\\\\[\"'])[^\\s'\"]++");

    // Credentials in a URL userinfo, e.g. jdbc:postgresql://user:pw@host. The bounded scheme and the bounded, greedy
    // password (matched up to the last '@' so a password containing '@' or '/' is still masked) keep this linear.
    private static final Pattern URL_CREDENTIALS = Pattern.compile(
            "([a-zA-Z][a-zA-Z0-9+.-]{0,31}://[^:/?#\\s@]+:)[^\\s]{1,256}@");

    private static final Pattern PWD_PARAM = Pattern.compile("(?i)(pwd=)[^\\s'\"]*");

    private static final Pattern TOKEN_ASSIGNMENT = Pattern.compile("(?i)(token[\"']?\\s*[:=]\\s*[\"']?)[^\\s'\"]+");

    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
            "(?i)(authorization\\s*:\\s*(?:bearer|basic|digest|negotiate)\\s+)\\S+");

    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(\\bbearer\\s+)\\S+");

    // AWS long-term (AKIA) and temporary STS (ASIA) access key ids.
    private static final Pattern AWS_ACCESS_KEY_ID = Pattern.compile("(?:AKIA|ASIA)[0-9A-Z]{16}");

    // Every pattern redact() masks with, AnonymizerUtil's included, so the size bound never cuts through a secret.
    private static final List<Pattern> SECRET_PATTERNS = Stream.concat(
            Stream.of(SECRET_VALUE_ESCAPED_DOUBLE_QUOTED, SECRET_VALUE_DOUBLE_QUOTED, SECRET_VALUE_SINGLE_QUOTED,
                    SECRET_VALUE_BARE, URL_CREDENTIALS, PWD_PARAM, TOKEN_ASSIGNMENT, AUTHORIZATION_HEADER, BEARER_TOKEN,
                    AWS_ACCESS_KEY_ID),
            AnonymizerUtil.getPatterns().stream()).toList();

    private static final Pattern NEW_LINE = Pattern.compile("\\r?\\n");

    private static final Pattern JAVA_EXCEPTION_LINE = Pattern.compile(
            "^\\s*([a-zA-Z_$][\\w$]*\\.)+[A-Z][\\w$]*(Exception|Error)(:.*)?$");

    public Optional<SanitizedRoleLog> sanitize(String rawLog) {
        try {
            if (rawLog == null || rawLog.isBlank()) {
                return Optional.empty();
            }
            String redacted = rawLog.length() > INPUT_CAP
                    ? dropLeadingPartialLine(redact(bound(rawLog)))
                    : redact(rawLog);
            String tail = buildTail(redacted);
            String extract = buildExtract(redacted);
            return Optional.of(new SanitizedRoleLog(extract, tail));
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to sanitize Cloudera Manager role log, returning empty result", e);
            return Optional.empty();
        }
    }

    // Cuts the input to about its last INPUT_CAP characters. A secret that starts before the cut and ends after it would
    // lose its marker and leak its value, so the cut is moved past the end of every such match. The leading partial
    // line is dropped only after redaction: a marker on that line may still own a value on the next one.
    private String bound(String input) {
        String window = input.substring(Math.max(0, input.length() - INPUT_CAP - REDACTION_OVERLAP));
        int cut = window.length() - INPUT_CAP;
        int previousCut;
        do {
            previousCut = cut;
            for (Pattern pattern : SECRET_PATTERNS) {
                cut = moveCutPastStraddlingMatch(pattern, window, cut);
            }
        } while (cut != previousCut);
        return window.substring(cut);
    }

    private int moveCutPastStraddlingMatch(Pattern pattern, String window, int cut) {
        Matcher matcher = pattern.matcher(window);
        int newCut = cut;
        while (matcher.find() && matcher.start() < newCut) {
            newCut = Math.max(newCut, matcher.end());
        }
        return newCut;
    }

    private String redact(String input) {
        // Mask structured (quoted / bare) secrets first, so a whole value is masked as one unit before AnonymizerUtil's
        // bare-word patterns would otherwise mask only up to the first whitespace and leak a space-separated value.
        String redacted = SECRET_VALUE_ESCAPED_DOUBLE_QUOTED.matcher(input).replaceAll("$1\\\\\"" + MASK + "\\\\\"");
        redacted = SECRET_VALUE_DOUBLE_QUOTED.matcher(redacted).replaceAll("$1\"" + MASK + "\"");
        redacted = SECRET_VALUE_SINGLE_QUOTED.matcher(redacted).replaceAll("$1'" + MASK + "'");
        redacted = SECRET_VALUE_BARE.matcher(redacted).replaceAll("$1" + MASK);
        redacted = AnonymizerUtil.anonymize(redacted);
        redacted = URL_CREDENTIALS.matcher(redacted).replaceAll("$1" + MASK + "@");
        redacted = PWD_PARAM.matcher(redacted).replaceAll("$1" + MASK);
        redacted = TOKEN_ASSIGNMENT.matcher(redacted).replaceAll("$1" + MASK);
        redacted = AUTHORIZATION_HEADER.matcher(redacted).replaceAll("$1" + MASK);
        redacted = BEARER_TOKEN.matcher(redacted).replaceAll("$1" + MASK);
        redacted = AWS_ACCESS_KEY_ID.matcher(redacted).replaceAll(MASK);
        return redacted;
    }

    private String buildTail(String redacted) {
        String tail = redacted;
        if (tail.length() > MAX_TAIL_LENGTH) {
            tail = dropLeadingPartialLine(tail.substring(tail.length() - MAX_TAIL_LENGTH));
        }
        return tail.strip();
    }

    private String buildExtract(String redacted) {
        String[] lines = NEW_LINE.split(redacted);
        String selected = selectCausedBy(lines);
        if (selected == null) {
            selected = selectExceptionLine(lines);
        }
        if (selected == null) {
            selected = selectLastNonBlankLine(lines);
        }
        if (selected == null) {
            return "";
        }
        return cut(selected.strip());
    }

    private String selectCausedBy(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].contains("Caused by:")) {
                return lines[i];
            }
        }
        return null;
    }

    private String selectExceptionLine(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            if (line.strip().startsWith("Exception in thread")
                    || (line.length() <= MAX_EXCEPTION_LINE_SCAN && JAVA_EXCEPTION_LINE.matcher(line).matches())) {
                return line;
            }
        }
        return null;
    }

    private String selectLastNonBlankLine(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) {
                return lines[i];
            }
        }
        return null;
    }

    private String dropLeadingPartialLine(String input) {
        int newLineIndex = input.indexOf('\n');
        if (newLineIndex >= 0) {
            return input.substring(newLineIndex + 1);
        }
        // No newline: drop the leading partial token too, so a fragment cut out of a longer token never surfaces.
        // Linear scan, no backtracking.
        int boundary = indexOfTokenBoundary(input);
        return boundary >= 0 ? input.substring(boundary) : "";
    }

    private int indexOfTokenBoundary(String input) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == ';' || c == '&') {
                return i;
            }
        }
        return -1;
    }

    private String cut(String line) {
        if (line.length() > MAX_EXTRACT_LENGTH) {
            return line.substring(0, MAX_EXTRACT_LENGTH) + ELLIPSIS;
        }
        return line;
    }

    /**
     * Immutable result of sanitizing a role log.
     *
     * @param extract one short, redacted line suitable for appending to a failure message
     * @param tail    the redacted last part of the log, suitable for logging
     */
    public record SanitizedRoleLog(String extract, String tail) {
    }
}
