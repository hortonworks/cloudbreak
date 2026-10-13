package com.sequenceiq.freeipa.salt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Executes freeipa_check_replication_cleanup.sh with stubbed ldapsearch/ldapdelete/sleep commands. The script prints its
 * "TIMEOUT after" line to stderr only when it gives up on its own timeout, and VerifyReplicationCleanupService relies on that
 * line to tell the timeout from a run that never evaluated replication.
 */
@EnabledOnOs(OS.LINUX)
class FreeIpaCheckReplicationCleanupScriptTest {

    private static final Path SCRIPT = Path.of("src/main/resources/freeipa-salt/salt/freeipa/scripts/freeipa_check_replication_cleanup.sh");

    private static final String REMOVED_HOST = "ipaserver1.example.com";

    private static final String DUMMY_PASSWORD = "dummy-password";

    private static final String MARKER = "TIMEOUT after";

    /**
     * The timeout line VerifyReplicationCleanupService matches on. VerifyReplicationCleanupServiceTest feeds the same line
     * (CLEANUP_TIMEOUT_OUTPUT) to the service to drive its timeout detection, so a change here must be mirrored there.
     */
    private static final Pattern TIMEOUT_LINE = Pattern.compile("^TIMEOUT after \\d+s waiting for FreeIPA replication cleanup on \\S+ for hosts '.*'\\.$");

    private static final String PENDING_TASK_FIXTURE = "pending-task";

    private static final String LINGERING_AGREEMENT_FIXTURE = "lingering-agreement";

    private static final String DELETE_REMOVES_FIXTURE = "delete-removes";

    private static final String STUB_FIXTURES_ENV = "STUB_FIXTURES";

    private static final String STUB_LDAPSEARCH = """
            #!/usr/bin/env bash
            base=""
            while [ $# -gt 0 ]; do
                case "$1" in
                    -b) base="$2"; shift 2;;
                    *) shift;;
                esac
            done
            case "$base" in
                "cn=cleanallruv,cn=tasks,cn=config")
                    if [ -e "$STUB_FIXTURES/pending-task" ]; then
                        echo "dn: cn=clean 8,cn=cleanallruv,cn=tasks,cn=config"
                        echo "objectClass: extensibleObject"
                    fi
                    ;;
                "cn=mapping tree,cn=config")
                    if [ -e "$STUB_FIXTURES/lingering-agreement" ]; then
                        echo "dn: cn=meToipaserver1.example.com,cn=replica,cn=dc\\3Dexample\\2Cdc\\3Dcom,cn=mapping tree,cn=config"
                        echo "nsDS5ReplicaHost: ipaserver1.example.com"
                    fi
                    ;;
            esac
            exit 0
            """;

    private static final String STUB_LDAPDELETE = """
            #!/usr/bin/env bash
            echo "$@" >> "$STUB_FIXTURES/ldapdelete.calls"
            if [ -e "$STUB_FIXTURES/delete-removes" ]; then
                rm -f "$STUB_FIXTURES/lingering-agreement"
            fi
            exit 0
            """;

    private static final String STUB_SLEEP = """
            #!/usr/bin/env bash
            exit 0
            """;

    @TempDir
    Path binDir;

    @TempDir
    Path fixtureDir;

    @BeforeEach
    void setUp() throws IOException {
        writeStub("ldapsearch", STUB_LDAPSEARCH);
        writeStub("ldapdelete", STUB_LDAPDELETE);
        writeStub("sleep", STUB_SLEEP);
    }

    @Test
    void testPendingCleanAllRuvTaskThenTimeoutWithMarker() throws Exception {
        setFixture(PENDING_TASK_FIXTURE);

        ScriptResult result = runScript(REMOVED_HOST);

        assertThat(result.exitCode()).isEqualTo(1);
        assertTimeoutMarkerLine(result);
        assertThat(result.stderr()).contains("CleanAllRUV task still active");
        assertThat(result.stdout()).doesNotContain("SUCCESS");
    }

    @Test
    void testLingeringAgreementThenTimeoutWithMarker() throws Exception {
        setFixture(LINGERING_AGREEMENT_FIXTURE);

        ScriptResult result = runScript(REMOVED_HOST);

        assertThat(result.exitCode()).isEqualTo(1);
        assertTimeoutMarkerLine(result);
        assertThat(result.stderr()).contains("Lingering replication agreement(s) for host(s): " + REMOVED_HOST);
        assertThat(result.stdout()).contains("Removing lingering replication agreement").doesNotContain("SUCCESS");
    }

    @Test
    void testConvergedThenSuccessWithoutMarker() throws Exception {
        ScriptResult result = runScript(REMOVED_HOST);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS");
        assertNoMarker(result);
    }

    @Test
    void testLingeringAgreementRemovedByDeleteThenSuccessWithoutMarker() throws Exception {
        setFixture(LINGERING_AGREEMENT_FIXTURE);
        setFixture(DELETE_REMOVES_FIXTURE);

        ScriptResult result = runScript(REMOVED_HOST);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS");
        assertThat(fixtureDir.resolve("ldapdelete.calls")).exists();
        assertNoMarker(result);
    }

    @Test
    void testEmptyTargetHostsThenExitZeroWithoutMarker() throws Exception {
        setFixture(PENDING_TASK_FIXTURE);

        ScriptResult result = runScript("");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("TARGET_HOSTS is empty");
        assertNoMarker(result);
    }

    @Test
    void testMissingFpwThenFailsBeforePollingWithoutMarker() throws Exception {
        setFixture(PENDING_TASK_FIXTURE);

        ScriptResult result = runScript(REMOVED_HOST, "FPW");

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.stderr()).contains("FPW");
        assertNoMarker(result);
    }

    private static void assertTimeoutMarkerLine(ScriptResult result) {
        assertThat(result.stderr()).contains(MARKER);
        assertThat(result.stderr().lines().filter(line -> TIMEOUT_LINE.matcher(line).find()).toList()).hasSize(1);
    }

    private static void assertNoMarker(ScriptResult result) {
        assertThat(result.stdout()).doesNotContain(MARKER);
        assertThat(result.stderr()).doesNotContain(MARKER);
    }

    private ScriptResult runScript(String targetHosts, String... unsetVariables) throws IOException, InterruptedException {
        assertThat(SCRIPT).exists();
        File stdoutFile = fixtureDir.resolve("script.stdout").toFile();
        File stderrFile = fixtureDir.resolve("script.stderr").toFile();
        ProcessBuilder processBuilder = new ProcessBuilder("bash", SCRIPT.toAbsolutePath().toString());
        processBuilder.environment().put("PATH", binDir + File.pathSeparator + System.getenv("PATH"));
        processBuilder.environment().put("FPW", DUMMY_PASSWORD);
        processBuilder.environment().put("TARGET_HOSTS", targetHosts);
        processBuilder.environment().put("TIMEOUT_SECONDS", "0");
        processBuilder.environment().put("POLL_INTERVAL_SECONDS", "0");
        processBuilder.environment().put(STUB_FIXTURES_ENV, fixtureDir.toAbsolutePath().toString());
        Arrays.stream(unsetVariables).forEach(processBuilder.environment()::remove);
        processBuilder.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
        processBuilder.redirectOutput(stdoutFile);
        processBuilder.redirectError(stderrFile);
        Process process = processBuilder.start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Script did not finish within 30 seconds, stdout so far: "
                    + Files.readString(stdoutFile.toPath(), StandardCharsets.UTF_8));
        }
        return new ScriptResult(process.exitValue(),
                Files.readString(stdoutFile.toPath(), StandardCharsets.UTF_8),
                Files.readString(stderrFile.toPath(), StandardCharsets.UTF_8));
    }

    private void writeStub(String name, String content) throws IOException {
        Path stub = binDir.resolve(name);
        Files.writeString(stub, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(stub, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private void setFixture(String name) throws IOException {
        Files.writeString(fixtureDir.resolve(name), "", StandardCharsets.UTF_8);
    }

    private record ScriptResult(int exitCode, String stdout, String stderr) {
    }
}
