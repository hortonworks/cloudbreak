package com.sequenceiq.cloudbreak.cm.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerRoleLogSanitizer.SanitizedRoleLog;

class ClouderaManagerRoleLogSanitizerTest {

    private final ClouderaManagerRoleLogSanitizer underTest = new ClouderaManagerRoleLogSanitizer();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n", "\t", " \n\t  "})
    void sanitizeReturnsEmptyForNullBlankInput(String input) {
        assertThat(underTest.sanitize(input)).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("redactionCases")
    void sanitizeRedactsSecrets(String description, String input, String secret, String expectedPresent) {
        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail())
                .as("secret '%s' must be removed from tail (%s)", secret, description)
                .doesNotContain(secret);
        assertThat(result.tail())
                .as("expected redacted/preserved text '%s' must remain in tail (%s)", expectedPresent, description)
                .contains(expectedPresent);
    }

    static Stream<Arguments> redactionCases() {
        return Stream.of(
                // --- new stderr-specific patterns added by the sanitizer ---
                Arguments.of("url-credentials",
                        "Connecting to postgresql://admin:TopSecret99@db.host:5432/app",
                        "TopSecret99", "postgresql://admin:****@db.host"),
                Arguments.of("jdbc-url-credentials",
                        "jdbc:postgresql://user1:secretPw99@dbhost:5432/db",
                        "secretPw99", "jdbc:postgresql://user1:****@dbhost"),
                Arguments.of("pwd-connection-param",
                        "jdbc url ?pwd=hunter2 moretext",
                        "hunter2", "pwd=****"),
                Arguments.of("jvm-token-property",
                        "running -Dauth.service.token=abc123XYZ end",
                        "abc123XYZ", "token=****"),
                Arguments.of("bare-token-assignment",
                        "config token=mytok9 following",
                        "mytok9", "token=****"),
                Arguments.of("authorization-bearer-header",
                        "Authorization: Bearer eyJhbGci.payload.sig",
                        "eyJhbGci.payload.sig", "Authorization: Bearer ****"),
                Arguments.of("authorization-basic-header",
                        "Authorization: Basic dXNlcjpwYXNzd29yZA==",
                        "dXNlcjpwYXNzd29yZA==", "Authorization: Basic ****"),
                Arguments.of("bare-bearer-token",
                        "retrying with Bearer tok12345 now",
                        "tok12345", "Bearer ****"),
                Arguments.of("json-spaced-colon",
                        "config { \"password\": \"hunter2secret\" } loaded",
                        "hunter2secret", "\"password\": \"****\""),
                Arguments.of("json-spaced-around-colon",
                        "config { \"password\" : \"zSecretOne\" } loaded",
                        "zSecretOne", "\"password\" : \"****\""),
                Arguments.of("json-nested-password-key-name",
                        "config { \"keystorePassword\" : \"topKeystorePw\" } loaded",
                        "topKeystorePw", "\"keystorePassword\" : \"****\""),
                Arguments.of("json-token-key-name",
                        "config { \"accessToken\": \"abc.def.ghi\" } loaded",
                        "abc.def.ghi", "\"accessToken\": \"****\""),
                Arguments.of("json-secret-access-key-name",
                        "config { \"secretAccessKey\": \"theSecretAccessVal\" } loaded",
                        "theSecretAccessVal", "\"secretAccessKey\": \"****\""),
                Arguments.of("json-private-key-name",
                        "config { \"privateKey\": \"superPrivateVal\" } loaded",
                        "superPrivateVal", "\"privateKey\": \"****\""),
                Arguments.of("json-escaped-quote-in-value",
                        "config {\"password\": \"ab\\\"cd\"} loaded",
                        "ab\\\"cd", "\"password\": \"****\""),
                Arguments.of("password-colon-space-bare-value",
                        "db config password: hunter2 done",
                        "hunter2", "password:****"),
                Arguments.of("password-colon-space-quoted-multiword-value",
                        "log password: \"multi word pw\" done",
                        "multi word pw", "\"****\""),
                Arguments.of("python-dict-single-quoted",
                        "stderr {'password': 'hunter2val'} loaded",
                        "hunter2val", "'password': '****'"),
                Arguments.of("escaped-json-double-quoted",
                        "config {\\\"password\\\":\\\"Hunter2\\\"} loaded",
                        "Hunter2", "\\\"password\\\":\\\"****\\\""),
                // --- cases already covered by AnonymizerUtil (must still be masked) ---
                Arguments.of("anonymizer-password-param",
                        "jdbc connect password=supersecret done",
                        "supersecret", "password=****"),
                Arguments.of("anonymizer-aws-secret-access-key",
                        "aws_secret_access_key=wJalrXUtnFEMIK7MDENG done",
                        "wJalrXUtnFEMIK7MDENG", "****"),
                Arguments.of("anonymizer-jvm-password-property",
                        "-Djavax.net.ssl.keyStorePassword=changeit tail",
                        "changeit", "****"),
                Arguments.of("anonymizer-jvm-secret-property",
                        "-Dfoo.secret=sekret99 tail",
                        "sekret99", "****"),
                Arguments.of("anonymizer-storepass-flag",
                        "keytool -storepass changeit99 next",
                        "changeit99", "****"),
                Arguments.of("anonymizer-keypass-flag",
                        "keytool -keypass topsecretkey",
                        "topsecretkey", "****"),
                Arguments.of("anonymizer-passin-flag",
                        "openssl -passin pass:mypass123 rest",
                        "mypass123", "****"),
                Arguments.of("anonymizer-passout-flag",
                        "openssl -passout pass:outpass456 rest",
                        "outpass456", "****")
        );
    }

    @Test
    void sanitizeRedactsBeforeTruncatingSoMarkerAtBoundaryStillMasks() {
        String secret = "SuperSecretPwdValue1234567890";
        // Single long line (no newlines): 'password=' sits in the first part that a tail-only truncation would drop,
        // while the secret value straddles/follows the last-MAX_TAIL_LENGTH window. If redaction ran AFTER truncation
        // the 'password=' marker would be gone and the value would leak. Correct order masks the full text first.
        int trailing = ClouderaManagerRoleLogSanitizer.MAX_TAIL_LENGTH - secret.length();
        String input = "a".repeat(200) + "password=" + secret + "b".repeat(trailing);

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail()).doesNotContain(secret);
        assertThat(result.tail()).contains("password=****");
    }

    @Test
    void sanitizeBoundsOversizedInputAndTailStartsAtLineBoundary() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (sb.length() < 70 * 1024) {
            sb.append(String.format("LINE%06d: some cloudera manager role stderr content padding padding padding%n", i++));
        }
        String input = sb.toString();
        assertThat(input.length()).isGreaterThan(ClouderaManagerRoleLogSanitizer.INPUT_CAP);

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail().length()).isLessThanOrEqualTo(ClouderaManagerRoleLogSanitizer.MAX_TAIL_LENGTH);
        assertThat(result.tail()).startsWith("LINE");
        assertThat(result.tail()).doesNotContain("LINE000000");
    }

    @Test
    void extractPrefersLastCausedBy() {
        String input = "Caused by: java.lang.IllegalStateException: first cause\n"
                + "\tat com.example.Foo.bar(Foo.java:1)\n"
                + "Caused by: java.io.IOException: second cause\n"
                + "\tat com.example.Baz.qux(Baz.java:2)";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).isEqualTo("Caused by: java.io.IOException: second cause");
    }

    @Test
    void extractFallsBackToExceptionLineWhenNoCausedBy() {
        String input = "starting role\n"
                + "com.example.service.ConfigException: boom while reading config\n"
                + "process exited with code 1";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).isEqualTo("com.example.service.ConfigException: boom while reading config");
    }

    @Test
    void extractFallsBackToExceptionInThreadLine() {
        String input = "noise line\n"
                + "Exception in thread \"main\" java.lang.NullPointerException\n"
                + "later unrelated line";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).startsWith("Exception in thread \"main\"");
    }

    @Test
    void extractFallsBackToLastNonBlankLineForNonJavaOutput() {
        String input = "first plain line\n"
                + "the meaningful last line of output\n"
                + "\n"
                + "   ";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).isEqualTo("the meaningful last line of output");
    }

    @Test
    void extractIsCappedWithEllipsis() {
        String longTail = "x".repeat(300);
        String input = "Caused by: java.lang.RuntimeException: " + longTail;

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).hasSize(ClouderaManagerRoleLogSanitizer.MAX_EXTRACT_LENGTH + 3);
        assertThat(result.extract()).endsWith("...");
        assertThat(result.extract()).startsWith("Caused by: java.lang.RuntimeException:");
    }

    @Test
    void extractIsRedacted() {
        String input = "Caused by: java.sql.SQLException: login failed for password=topSecretPw42";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).doesNotContain("topSecretPw42");
        assertThat(result.extract()).contains("password=****");
    }

    @Test
    void awsAccessKeyIdIsMaskedWhileContextIsPreserved() {
        String input = "role failed: principal AKIAIOSFODNN7EXAMPLE denied access to bucket";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail()).doesNotContain("AKIAIOSFODNN7EXAMPLE");
        assertThat(result.tail()).contains("principal ****").contains("denied access to bucket");
    }

    @Test
    void awsTemporaryStsAccessKeyIdIsMasked() {
        String input = "role failed: temp principal ASIAIOSFODNN7EXAMPLE denied access to bucket";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail()).doesNotContain("ASIAIOSFODNN7EXAMPLE");
        assertThat(result.tail()).contains("principal ****").contains("denied access to bucket");
    }

    @Test
    void urlUserinfoPasswordContainingAtOrSlashIsFullyMasked() {
        SanitizedRoleLog atResult = underTest.sanitize(
                "Connecting to postgresql://admin:p@ss@dbhost:5432/app").orElseThrow();
        SanitizedRoleLog slashResult = underTest.sanitize(
                "Connecting to postgresql://admin:ab/cd@dbhost:5432/app").orElseThrow();

        assertThat(atResult.tail())
                .doesNotContain("p@ss")
                .contains("postgresql://admin:****@dbhost");
        assertThat(slashResult.tail())
                .doesNotContain("ab/cd")
                .contains("postgresql://admin:****@dbhost");
    }

    @Test
    void sanitizeDropsLeadingPartialTokenWhenSingleHugeLineCutLandsInsideSecretMarker() {
        String secret = "LeakedSecretValue123";
        // One huge line with no newline, engineered so the last-INPUT_CAP window begins at 'word=' — i.e. the cut
        // splits 'password=' into 'pass' (dropped) and 'word=<secret>'. The redactor can no longer recognise the
        // marker, so the leading partial token must be dropped up to the first whitespace or the secret would leak.
        int cap = ClouderaManagerRoleLogSanitizer.INPUT_CAP;
        String suffixFromWord = "word=" + secret + " " + "b".repeat(cap - "word=".length() - secret.length() - 1);
        String input = "a".repeat(10) + "pass" + suffixFromWord;
        assertThat(input.length()).isGreaterThan(cap);

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail()).doesNotContain(secret).doesNotContain("word=");
        assertThat(result.extract()).doesNotContain(secret).doesNotContain("word=");
    }

    @Test
    void quotedSecretValueIsFullyMaskedForMultiWordValue() {
        String doubleQuoted = "launch args password=\"multi word value here\" --verbose";
        String singleQuoted = "launch args credential='another multi word secret' --verbose";

        SanitizedRoleLog doubleResult = underTest.sanitize(doubleQuoted).orElseThrow();
        SanitizedRoleLog singleResult = underTest.sanitize(singleQuoted).orElseThrow();

        assertThat(doubleResult.tail())
                .doesNotContain("multi word value here")
                .doesNotContain("word value here")
                .contains("password=\"****\"")
                .contains("--verbose");
        assertThat(singleResult.tail())
                .doesNotContain("another multi word secret")
                .doesNotContain("multi word secret")
                .contains("credential='****'")
                .contains("--verbose");
    }

    @Test
    void extractHandlesCrlfLineEndings() {
        String input = "starting role\r\n"
                + "Caused by: java.io.IOException: disk full\r\n"
                + "\tat com.example.Writer.write(Writer.java:3)\r\n";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).isEqualTo("Caused by: java.io.IOException: disk full");
        assertThat(result.extract()).doesNotContain("\r");
    }

    @Test
    void tailHandlesCrlfLineBoundary() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (sb.length() < 70 * 1024) {
            sb.append(String.format("LINE%06d: some cloudera manager role stderr content padding padding padding", i++)).append("\r\n");
        }
        String input = sb.toString();

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.tail().length()).isLessThanOrEqualTo(ClouderaManagerRoleLogSanitizer.MAX_TAIL_LENGTH);
        assertThat(result.tail()).startsWith("LINE");
        assertThat(result.tail()).doesNotContain("LINE000000");
    }

    @Test
    void sanitizeDoesNotThrowOnPathologicalSingleLineInput() {
        String longLine = "a".repeat(100_000);
        String manyEquals = "=".repeat(100_000);
        String manyPasswords = "password=".repeat(20_000);
        // Targeted at URL_CREDENTIALS: a scheme followed by many ':' and no '@' (kept under the input cap so the
        // scheme is actually exercised rather than trimmed away by bounding).
        String urlNoAt = "a://" + ":".repeat(60_000);
        // Targeted at TOKEN_ASSIGNMENT: a token marker then a long run with no terminator.
        String tokenNoEnd = "token=" + "a".repeat(60_000);
        // Targeted at JAVA_EXCEPTION_LINE: a long dotted identifier (within the 2048 scan limit) with no Exception suffix.
        String dottedNoException = "a.".repeat(1_000) + "end";
        // Targeted at SECRET_VALUE_DOUBLE_QUOTED: a quoted secret value that never closes.
        String quotedNoClose = "password=\"" + "a".repeat(60_000);
        // Targeted at URL_CREDENTIALS greedy password: a userinfo with a long run of '@' and no host.
        String urlManyAt = "a://u:" + "@".repeat(60_000);

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            assertThat(underTest.sanitize(longLine)).isPresent();
            assertThat(underTest.sanitize(manyEquals)).isPresent();
            assertThat(underTest.sanitize(urlNoAt)).isPresent();
            assertThat(underTest.sanitize(tokenNoEnd)).isPresent();
            assertThat(underTest.sanitize(dottedNoException)).isPresent();
            assertThat(underTest.sanitize(quotedNoClose)).isPresent();
            assertThat(underTest.sanitize(urlManyAt)).isPresent();
            Optional<SanitizedRoleLog> result = underTest.sanitize(manyPasswords);
            assertThat(result).isPresent();
            assertThat(result.orElseThrow().tail().length()).isLessThanOrEqualTo(ClouderaManagerRoleLogSanitizer.MAX_TAIL_LENGTH);
        });
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("hugeSingleLineSplitMarkerCases")
    void sanitizeDoesNotLeakWhenHugeSingleLineCutSplitsSecretMarker(
            String description, String beforeKey, String afterPass, String extraForbidden) {
        int cap = ClouderaManagerRoleLogSanitizer.INPUT_CAP;
        // One huge line, engineered so the last-INPUT_CAP cut lands between 'pass' and 'word', splitting the 'password'
        // marker. The window reaches REDACTION_OVERLAP chars in front of the cut, sees the whole marker and its value,
        // and moves the cut past them, so the value never reaches extract or tail.
        String prefix = "a".repeat(ClouderaManagerRoleLogSanitizer.REDACTION_OVERLAP) + beforeKey + "pass";
        String suffix = afterPass + "b".repeat(cap - afterPass.length());
        String input = prefix + suffix;
        assertThat(input.length()).isGreaterThan(cap);

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).doesNotContain("Hunter2");
        assertThat(result.tail()).doesNotContain("Hunter2");
        if (extraForbidden != null) {
            assertThat(result.extract()).doesNotContain(extraForbidden);
            assertThat(result.tail()).doesNotContain(extraForbidden);
        }
    }

    static Stream<Arguments> hugeSingleLineSplitMarkerCases() {
        return Stream.of(
                Arguments.of("colon-bare-value", "", "word: Hunter2 ", null),
                Arguments.of("json-double-quoted", "{\"", "word\": \"Hunter2\" ", null),
                Arguments.of("double-quoted-multiword", "", "word=\"first Hunter2\" ", "first"),
                Arguments.of("bare-equals-value", "", "word=Hunter2 ", null),
                // Space-separated value: the AnonymizerUtil 'password ' marker is split by the cut, but the straddle
                // check sees the whole marker + value via its patterns and moves the cut past it.
                Arguments.of("space-separated-value", "", "word Hunter2 ", null),
                // AnonymizerUtil's CM name/value pattern split through the 'password' inside the "name" field.
                Arguments.of("cm-name-value-json", "{\"name\": \"db_", "word\", \"value\": \"Hunter2\"} ", null),
                // Escaped JSON (JSON embedded in a JSON string) split through the 'password' key.
                Arguments.of("escaped-json-double-quoted", "{\\\"", "word\\\":\\\"Hunter2\\\"} ", null)
        );
    }

    @Test
    void escapedJsonSecretIsMaskedWhileOtherFieldsRemain() {
        // Literal backslashes in the log text: escaped JSON inside a JSON string, {\"password\":\"Hunter2\"}.
        SanitizedRoleLog single = underTest.sanitize("config {\\\"password\\\":\\\"Hunter2\\\"} loaded").orElseThrow();
        assertThat(single.tail()).doesNotContain("Hunter2").contains("\\\"password\\\":\\\"****\\\"");

        SanitizedRoleLog multi = underTest.sanitize("config {\\\"password\\\":\\\"Hunter2\\\",\\\"user\\\":\\\"bob\\\"} loaded").orElseThrow();
        assertThat(multi.tail()).doesNotContain("Hunter2").contains("\\\"password\\\":\\\"****\\\"").contains("bob");

        // Value with an embedded escaped quote (ab\"cd, i.e. 3 backslashes + quote): the escaped quote must be consumed
        // whole, so the value is masked past it and both halves disappear.
        SanitizedRoleLog escapedQuote = underTest.sanitize("config {\\\"password\\\":\\\"ab\\\\\\\"cd\\\"} loaded").orElseThrow();
        assertThat(escapedQuote.tail()).doesNotContain("ab").doesNotContain("cd").contains("{\\\"password\\\":\\\"****\\\"}");

        // Value ending in an escaped backslash (ab\\, i.e. 4 backslashes): the escaped backslash must be consumed whole
        // so the following \" is recognised as the real close, masking only the value and leaving the next field intact.
        SanitizedRoleLog escapedBackslash =
                underTest.sanitize("config {\\\"password\\\":\\\"ab\\\\\\\\\\\",\\\"u\\\":\\\"bob\\\"} loaded").orElseThrow();
        assertThat(escapedBackslash.tail()).doesNotContain("ab").contains("\\\"password\\\":\\\"****\\\"").contains("bob");
    }

    @Test
    void sanitizeDoesNotLeakWhenCutSplitsMarkerWithValueOnNextLine() {
        int cap = ClouderaManagerRoleLogSanitizer.INPUT_CAP;
        // Multi-line log whose last-INPUT_CAP cut falls inside the 'password' key, with the value on the next line. The
        // window matches 'password:' together with the value across the newline and moves the cut past it, so the value
        // is gone and the following 'Caused by:' line still becomes the extract.
        String core = "word:\nHunter2\nCaused by: x.y.FooException: boom\n";
        String suffix = core + "b".repeat(cap - core.length());
        String input = "a".repeat(ClouderaManagerRoleLogSanitizer.REDACTION_OVERLAP) + "pass" + suffix;
        assertThat(input.length()).isGreaterThan(cap);

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).contains("FooException").doesNotContain("Hunter2");
        assertThat(result.tail()).doesNotContain("Hunter2");
    }

    @Test
    void unclosedDoubleQuotedSecretStopsAtLineEnd() {
        String input = "secret: \"abc\n"
                + "Caused by: com.foo.BarException: real cause\n"
                + "last";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).contains("com.foo.BarException: real cause");
        assertThat(result.extract()).doesNotContain("abc");
        assertThat(result.tail()).doesNotContain("abc");
    }

    @Test
    void unclosedSingleQuotedSecretStopsAtLineEnd() {
        String input = "key: 'abc\n"
                + "Caused by: com.foo.BarException: real cause\n"
                + "don't stop";

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).contains("com.foo.BarException: real cause");
        assertThat(result.tail()).contains("don't stop");
        assertThat(result.extract()).doesNotContain("abc");
        assertThat(result.tail()).doesNotContain("abc");
    }

    @Test
    void sanitizeDoesNotLeakWhenInputLengthIsWithinRedactionOverlapOfCap() {
        int cap = ClouderaManagerRoleLogSanitizer.INPUT_CAP;
        int overlap = ClouderaManagerRoleLogSanitizer.REDACTION_OVERLAP;
        String secret = "StraddleLeak999";
        String marker = "password=" + secret;
        // Total length sits between INPUT_CAP and INPUT_CAP + REDACTION_OVERLAP, so the whole input is the window and
        // the cut falls inside the straddling marker, which must still be moved past so nothing leaks.
        int trailing = cap - marker.length() + 4;
        String input = "a".repeat(overlap / 2) + marker + " " + "b".repeat(trailing);
        assertThat(input.length()).isGreaterThan(cap);
        assertThat(input.length()).isLessThan(cap + overlap);

        SanitizedRoleLog result = underTest.sanitize(input).orElseThrow();

        assertThat(result.extract()).doesNotContain(secret);
        assertThat(result.tail()).doesNotContain(secret);
    }
}
