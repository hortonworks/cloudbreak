package com.sequenceiq.freeipa.salt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Executes freeipa_verify_replica_health.sh with stubbed hostname/kinit/klist/kdestroy/ldapsearch commands.
 * Topology: the script runs on ipaserver2 (the new replica), ipaserver0 is its seed and ipaserver1 is a master
 * without an agreement to ipaserver2.
 */
@EnabledOnOs(OS.LINUX)
class FreeIpaVerifyReplicaHealthScriptTest {

    private static final Path SCRIPT = Path.of("src/main/resources/freeipa-salt/salt/freeipa/scripts/freeipa_verify_replica_health.sh");

    private static final String DOMAIN = "example.com";

    private static final String LOCAL_HOST = "ipaserver2." + DOMAIN;

    private static final String SEED_HOST = "ipaserver0." + DOMAIN;

    private static final String OTHER_HOST = "ipaserver1." + DOMAIN;

    private static final String OK_STATUS = "Error (0) Replica acquired successfully: Incremental update succeeded";

    private static final String IDLE_STATUS = "Error (0) No replication sessions started since server startup";

    private static final String CONNECT_ERROR_STATUS = "Error (-1) Problem connecting to replica - LDAP error: Can't contact LDAP server (connection error)";

    private static final String INVALID_CREDENTIALS_STATUS =
            "Error (49) Replication error acquiring replica: Unable to acquire replica: LDAP error: Invalid credentials (Invalid credentials)";

    private static final String BUSY_STATUS = "Error (1) Can't acquire busy replica";

    private static final String DECODING_ERROR_STATUS = "Error (4) Replication error acquiring replica: decoding error";

    private static final String ABORTING_STATUS = "Error (4) Replication error acquiring replica: Replication is aborting";

    private static final String TRANSIENT_STATUS =
            "Error (18) Can't acquire replica (Incremental update transient warning.  Backing off, will retry update later.)";

    private static final String GENERATION = "6adf12a5000000060000";

    private static final String OTHER_GENERATION = "6adf12a5000000070000";

    private static final String BASE_TIME = "6adf12a9";

    private static final String OLD_TIME = "6adf3d26";

    private static final String NEW_TIME = "6adf3ee7";

    private static final int LOCAL_RID = 3;

    private static final int SEED_RID = 5;

    private static final int REMOVED_RID = 7;

    private static final int CLEANED_RID = 8;

    private static final String REAL_END = "20261021103000Z";

    private static final String EPOCH_END = "19700101000000Z";

    private static final String CA_AGMT_TO_SEED = "caTo" + SEED_HOST;

    private static final String CA_AGMT_TO_LOCAL = "caTo" + LOCAL_HOST;

    /**
     * The timeout line VerifyReplicationCleanupService matches on. VerifyReplicationCleanupServiceTest feeds the same line
     * (CONVERGENCE_TIMEOUT_OUTPUT) to the service to drive its timeout detection, so a change here must be mirrored there.
     */
    private static final Pattern TIMEOUT_LINE = Pattern.compile("^FAIL: replica \\S+ not healthy within \\d+s \\(see failing agreements\\)$");

    private static final String TIMEOUT_TEXT = "not healthy within";

    private static final String STUB_FIXTURES_ENV = "STUB_FIXTURES";

    private static final String STUB_HOSTNAME = """
            #!/usr/bin/env bash
            echo ipaserver2.example.com
            """;

    private static final String STUB_NOOP = """
            #!/usr/bin/env bash
            cat >/dev/null
            exit 0
            """;

    private static final String STUB_KINIT_FAILING = """
            #!/usr/bin/env bash
            cat >/dev/null
            echo "kinit: Preauthentication failed while getting initial credentials" >&2
            exit 1
            """;

    private static final String STUB_KLIST_NO_TICKET = """
            #!/usr/bin/env bash
            exit 1
            """;

    private static final String STUB_LDAPSEARCH = """
            #!/usr/bin/env bash
            uri=""
            base=""
            while [ $# -gt 0 ]; do
                case "$1" in
                    -H) uri="$2"; shift 2;;
                    -b) base="$2"; shift 2;;
                    *) shift;;
                esac
            done
            case "$base" in
                cn=masters*)
                    cat "$STUB_FIXTURES/masters.ldif"
                    ;;
                "cn=mapping tree,cn=config")
                    case "$uri" in
                        ldapi://*) name=local;;
                        ldap://*) name="${uri#ldap://}";;
                        *) name=unknown;;
                    esac
                    if [ -e "$STUB_FIXTURES/$name.fail" ]; then
                        echo "ldap_sasl_interactive_bind: Can't contact LDAP server (-1)" >&2
                        exit 255
                    fi
                    if [ -f "$STUB_FIXTURES/$name.ldif" ]; then
                        cat "$STUB_FIXTURES/$name.ldif"
                    fi
                    ;;
            esac
            exit 0
            """;

    @TempDir
    Path binDir;

    @TempDir
    Path fixtureDir;

    @BeforeEach
    void setUp() throws IOException {
        writeStub("hostname", STUB_HOSTNAME);
        writeStub("kinit", STUB_NOOP);
        writeStub("klist", STUB_NOOP);
        writeStub("kdestroy", STUB_NOOP);
        writeStub("ldapsearch", STUB_LDAPSEARCH);
        writeMasters(SEED_HOST, OTHER_HOST, LOCAL_HOST);
        writeLocal(ok(Suffix.IPA, SEED_HOST), ok(Suffix.CA, SEED_HOST));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST));
        writePeer(OTHER_HOST, ok(Suffix.IPA, SEED_HOST), ok(Suffix.CA, SEED_HOST));
    }

    @Test
    void testAllHealthyThenSuccess() throws Exception {
        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS");
        assertThat(result.stdout()).doesNotContain("idle agmt=");
        assertThat(result.stdout()).doesNotContain("RESULT:");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testLocalIdleAgreementCorroboratedBySiblingSuffixAndPeerDirectionThenSuccess() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS", "corroborated");
        assertThat(result.stdout()).doesNotContain("not corroborated");
    }

    @Test
    void testPeerIdleAgreementCorroboratedThenSuccess() throws Exception {
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), idle(Suffix.CA, LOCAL_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS", "corroborated");
    }

    @Test
    void testCodeZeroNoSessionsWithRealEndTimestampIsNotIdle() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), agreement(Suffix.CA, SEED_HOST, IDLE_STATUS, REAL_END, "FALSE"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).doesNotContain("idle agmt=");
    }

    @Test
    void testIdleAgreementOnBothSidesNotCorroboratedThenExitThree() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), idle(Suffix.CA, LOCAL_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).contains("not corroborated");
        assertThat(result.stdout()).doesNotContain("SUCCESS");
        assertSingleLineFailureMessage(result);
        assertResultLine(result,
                notCorroborated(CA_AGMT_TO_SEED, LOCAL_HOST, SEED_HOST, "o=ipaca"),
                notCorroborated(CA_AGMT_TO_LOCAL, SEED_HOST, LOCAL_HOST, "o=ipaca"));
    }

    @Test
    void testIdleAgreementWithFailingReverseDirectionThenExitOne() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), agreement(Suffix.CA, LOCAL_HOST, CONNECT_ERROR_STATUS, EPOCH_END, "FALSE"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testExplicitlyEnabledIdleAgreementCorroboratedThenSuccess() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST).replace("\n\n", "\nnsds5ReplicaEnabled: ON\n\n"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("corroborated");
    }

    @Test
    void testIdleAgreementWithoutUpdateEndAttributeCorroboratedThenSuccess() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST).replace("nsds5replicaLastUpdateEnd: " + EPOCH_END + "\n", ""));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("corroborated");
    }

    @Test
    void testIdleAgreementNextToBusySiblingSuffixThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), idle(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("not corroborated");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testDisabledAgreementWithNoSessionsStatusIsNotIdleThenExitOne() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST).replace("\n\n", "\nnsds5ReplicaEnabled: off\n\n"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("idle agmt=");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testAllLocalAgreementsIdleThenExitOne() throws Exception {
        writeLocal(idle(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        // Unresolved idle agreements are collected, but with nothing proving this dirsrv can bind outward the exit stays 1.
        assertThat(result.stdout()).contains("not corroborated").doesNotContain("RESULT:");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testIdleAgreementWithSiblingSuffixOkTowardOtherHostOnlyThenExitThree() throws Exception {
        writeLocal(ok(Suffix.IPA, OTHER_HOST), idle(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).contains("not corroborated");
        assertSingleLineFailureMessage(result);
        assertResultLine(result, notCorroborated(CA_AGMT_TO_SEED, LOCAL_HOST, SEED_HOST, "o=ipaca"));
    }

    @Test
    void testIdleAgreementWithoutSuffixAttributeThenExitThree() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST).replace("nsDS5ReplicaRoot: " + Suffix.CA.dn + "\n", ""));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).contains("not corroborated");
        assertSingleLineFailureMessage(result);
        assertResultLine(result, notCorroborated(CA_AGMT_TO_SEED, LOCAL_HOST, SEED_HOST, ""));
    }

    @Test
    void testNoSessionsStatusWithoutInProgressAttributeIsNotIdleThenExitOne() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST).replace("nsds5replicaUpdateInProgress: FALSE\n", ""));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("idle agmt=");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testIdleAgreementCorroboratedDespiteDifferentCaseThenSuccess() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST).replace("nsDS5ReplicaHost: ", "NSDS5REPLICAHOST: "), idle(Suffix.CA, SEED_HOST));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST),
                ok(Suffix.CA, LOCAL_HOST.toUpperCase()).replace("nsDS5ReplicaRoot: o=ipaca", "nsDS5ReplicaRoot: O=ipaca"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS", "corroborated");
        assertThat(result.stdout()).doesNotContain("not corroborated");
    }

    @Test
    void testIdleAgreementWithUnqueryablePeerThenNotHealthy() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));
        markUnqueryable(SEED_HOST);

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).doesNotContain("SUCCESS");
        assertSingleLineFailureMessage(result);
        assertResultLine(result,
                "peer " + SEED_HOST + " could not be queried",
                notCorroborated(CA_AGMT_TO_SEED, LOCAL_HOST, SEED_HOST, "o=ipaca"));
    }

    @Test
    void testUnqueryablePeerWithHealthyLocalAgreementsThenExitThreeNamingThePeerOnly() throws Exception {
        markUnqueryable(SEED_HOST);

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).doesNotContain("SUCCESS");
        assertSingleLineFailureMessage(result);
        assertResultLine(result, "peer " + SEED_HOST + " could not be queried");
    }

    @Test
    void testPeerHostingNoAgreementsWithHealthyLocalAgreementsThenExitThreeNamingThePeerOnly() throws Exception {
        removePeerFixture(SEED_HOST);

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).doesNotContain("SUCCESS");
        assertSingleLineFailureMessage(result);
        assertResultLine(result, "peer " + SEED_HOST + " hosts no replication agreement");
    }

    @Test
    void testFailedKinitThenEveryPeerQueryFailsAndExitThreeNamesKinitAndPeers() throws Exception {
        writeStub("klist", STUB_KLIST_NO_TICKET);
        writeStub("kinit", STUB_KINIT_FAILING);
        markUnqueryable(SEED_HOST);
        markUnqueryable(OTHER_HOST);

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).contains("kinit as admin failed").doesNotContain("SUCCESS");
        assertSingleLineFailureMessage(result);
        assertResultLine(result,
                "kinit as admin failed, so no peer could be queried",
                "peer " + SEED_HOST + " could not be queried",
                "peer " + OTHER_HOST + " could not be queried");
    }

    @Test
    void testIdleAgreementWithPeerHostingNoAgreementsThenNotHealthy() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));
        removePeerFixture(SEED_HOST);

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stdout()).doesNotContain("SUCCESS");
        assertSingleLineFailureMessage(result);
        assertResultLine(result,
                "peer " + SEED_HOST + " hosts no replication agreement",
                notCorroborated(CA_AGMT_TO_SEED, LOCAL_HOST, SEED_HOST, "o=ipaca"));
    }

    @Test
    void testBusyReplicaWithEpochEndThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("FAIL agmt=").doesNotContain("RESULT:");
        assertSingleLineFailureMessage(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {CONNECT_ERROR_STATUS, INVALID_CREDENTIALS_STATUS, DECODING_ERROR_STATUS, ABORTING_STATUS})
    void testLocalAgreementWithErrorStatusThenExitOne(String status) throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, status, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertSingleLineFailureMessage(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {CONNECT_ERROR_STATUS, INVALID_CREDENTIALS_STATUS, DECODING_ERROR_STATUS, ABORTING_STATUS})
    void testPeerAgreementWithErrorStatusThenExitOne(String status) throws Exception {
        writePeer(SEED_HOST, agreement(Suffix.IPA, LOCAL_HOST, status, EPOCH_END, "FALSE"), ok(Suffix.CA, LOCAL_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertSingleLineFailureMessage(result);
    }

    @ParameterizedTest
    @MethodSource("noSessionsButNotIdleAgreements")
    void testNoSessionsStatusThatIsNotIdleThenExitOne(String inProgress, String end) throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, IDLE_STATUS, end, inProgress), ok(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("idle agmt=");
        assertSingleLineFailureMessage(result);
    }

    static Stream<Arguments> noSessionsButNotIdleAgreements() {
        return Stream.of(
                Arguments.of("TRUE", EPOCH_END),
                Arguments.of("TRUE", REAL_END));
    }

    @Test
    void testInProgressAgreementIsPolledUntilTimeoutThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, OK_STATUS, REAL_END, "TRUE"), ok(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(2, 1);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(countOccurrences(result.stdout(), "== local")).isGreaterThanOrEqualTo(2);
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testLocalEndpointWithoutAgreementsThenExitOne() throws Exception {
        Files.delete(fixtureDir.resolve("local.ldif"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("RESULT:");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testBusyLocalAgreementWithCoveringSeedRuvThenStaleStatusIgnoredAndSuccess() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, OLD_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("is stale", "SUCCESS");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testTransientPeerAgreementWithCoveringLocalRuvThenStaleStatusIgnoredAndSuccess() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), agreement(Suffix.CA, LOCAL_HOST, TRANSIENT_STATUS, EPOCH_END, "FALSE"),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, OLD_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("is stale", "SUCCESS");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testPendingAgreementWhenSupplierListsRidMissingOnConsumerThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME), rid(REMOVED_RID, OLD_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("does not hold every change").doesNotContain("is stale", "SUCCESS");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testPendingAgreementWhenConsumerHasOlderMaxCsnThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, OLD_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("does not hold every change").doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testPendingAgreementWhenReplicaGenerationDiffersThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(OTHER_GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {CONNECT_ERROR_STATUS, INVALID_CREDENTIALS_STATUS})
    void testErrorStatusThatDoesNotProveBindWithCoveringRuvsThenExitOne(String status) throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, status, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("is stale", "pend agmt=");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testDisabledBusyAgreementWithCoveringRuvsThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE").replace("\n\n", "\nnsds5ReplicaEnabled: off\n\n"),
                ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("is stale", "pend agmt=");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testCodeZeroInProgressAgreementWithCoveringRuvsThenSuccess() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, OK_STATUS, REAL_END, "TRUE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("is stale", "SUCCESS");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testPendingAgreementWhenConsumerHasNoReplicaEntryThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testPendingAgreementWhenConsumerHasRuvOnlyForOtherSuffixThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testUncoveredPendingPeerAgreementWithEverythingElseHealthyThenExitOneNotThree() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, OLD_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), agreement(Suffix.CA, LOCAL_HOST, TRANSIENT_STATUS, EPOCH_END, "FALSE"),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.exitCode()).isNotEqualTo(3);
        assertThat(result.stdout()).contains("does not hold every change").doesNotContain("RESULT:");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testSupplierRidWithoutCsnsMissingOnConsumerThenStillConverged() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME), ridWithoutCsns(REMOVED_RID)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("is stale", "SUCCESS");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testTransientLocalAgreementWhenSupplierListsCleanedRidWithCsnsMissingOnSeedThenExitOne() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), agreement(Suffix.CA, SEED_HOST, TRANSIENT_STATUS, EPOCH_END, "FALSE"),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME), rid(CLEANED_RID, OLD_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("does not hold every change").doesNotContain("is stale", "SUCCESS");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testRuvElementsWithoutUrlOrWithTrailingTokenAreParsedThenStaleStatusIgnoredAndSuccess() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), ridWithoutUrl(LOCAL_RID, NEW_TIME), ridWithTrailingToken(SEED_RID, OLD_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), ridWithTrailingToken(LOCAL_RID, NEW_TIME), ridWithoutUrl(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("is stale", "SUCCESS");
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testRuvElementsWithoutUrlOrWithTrailingTokenWhenConsumerHasOlderMaxCsnThenExitOne() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.IPA, generation(GENERATION), ridWithoutUrl(LOCAL_RID, NEW_TIME), ridWithTrailingToken(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST),
                replica(Suffix.IPA, generation(GENERATION), ridWithTrailingToken(LOCAL_RID, NEW_TIME), ridWithoutUrl(SEED_RID, OLD_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("does not hold every change").doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testPendingLocalAgreementWhenPeerReturnsNoReplicaEntryThenFailureNamesPeer() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), agreement(Suffix.CA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), ok(Suffix.CA, LOCAL_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("no RUV of o=ipaca could be read from " + SEED_HOST).doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testPendingPeerAgreementWhenPeerReturnsNoReplicaEntryThenFailureNamesPeer() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), ok(Suffix.CA, SEED_HOST),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), agreement(Suffix.CA, LOCAL_HOST, TRANSIENT_STATUS, EPOCH_END, "FALSE"));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).contains("no RUV of o=ipaca could be read from " + SEED_HOST).doesNotContain("is stale");
        assertSingleLineFailureMessage(result);
    }

    @Test
    void testPendingAgreementsInBothDirectionsOnSameSuffixWithCoveringRuvsThenBothStaleAndSuccess() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), agreement(Suffix.CA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), agreement(Suffix.CA, LOCAL_HOST, TRANSIENT_STATUS, EPOCH_END, "FALSE"),
                replica(Suffix.CA, generation(GENERATION), rid(LOCAL_RID, NEW_TIME), rid(SEED_RID, NEW_TIME)));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("SUCCESS");
        assertThat(countOccurrences(result.stdout(), "is stale")).isEqualTo(2);
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void testTimeoutWithExitOnePrintsConvergenceTimeoutMarker() throws Exception {
        writeLocal(agreement(Suffix.IPA, SEED_HOST, BUSY_STATUS, EPOCH_END, "FALSE"), ok(Suffix.CA, SEED_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(1);
        assertTimeoutMarkerLine(result);
    }

    @Test
    void testTimeoutWithExitThreePrintsConvergenceTimeoutMarker() throws Exception {
        writeLocal(ok(Suffix.IPA, SEED_HOST), idle(Suffix.CA, SEED_HOST));
        writePeer(SEED_HOST, ok(Suffix.IPA, LOCAL_HOST), idle(Suffix.CA, LOCAL_HOST));

        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isEqualTo(3);
        assertTimeoutMarkerLine(result);
    }

    @Test
    void testSuccessDoesNotPrintConvergenceTimeoutMarker() throws Exception {
        ScriptResult result = runScript(0, 0);

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).doesNotContain(TIMEOUT_TEXT);
        assertThat(result.stderr()).doesNotContain(TIMEOUT_TEXT);
    }

    @Test
    void testMissingRealmFailsBeforePollingWithoutConvergenceTimeoutMarker() throws Exception {
        ScriptResult result = runScript(0, 0, "REALM");

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.stderr()).contains("REALM");
        assertThat(result.stdout()).doesNotContain(TIMEOUT_TEXT);
        assertThat(result.stderr()).doesNotContain(TIMEOUT_TEXT);
    }

    private static void assertTimeoutMarkerLine(ScriptResult result) {
        assertThat(result.stderr()).contains(TIMEOUT_TEXT);
        assertThat(result.stderr().lines().filter(line -> TIMEOUT_LINE.matcher(line).find()).toList()).hasSize(1);
    }

    private static String notCorroborated(String agreement, String supplier, String target, String suffix) {
        return "idle agreement " + agreement + " on " + supplier + " -> " + target + " (" + suffix + ") not corroborated";
    }

    private static void assertResultLine(ScriptResult result, String... unresolvedReasons) {
        List<String> resultLines = result.stdout().lines().filter(line -> line.startsWith("RESULT:")).toList();
        assertThat(resultLines).containsExactly("RESULT: no agreement is broken; unresolved: " + String.join("; ", unresolvedReasons));
    }

    private static void assertSingleLineFailureMessage(ScriptResult result) {
        List<String> stderrLines = result.stderr().lines().toList();
        assertThat(stderrLines).hasSize(1);
        assertThat(stderrLines.get(0)).startsWith("FAIL: replica " + LOCAL_HOST + " not healthy within");
        assertThat(result.stderr()).doesNotContain("agmt=").doesNotContain("CB-");
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private ScriptResult runScript(int timeoutSeconds, int pollIntervalSeconds) throws IOException, InterruptedException {
        return runScript(timeoutSeconds, pollIntervalSeconds, new String[0]);
    }

    private ScriptResult runScript(int timeoutSeconds, int pollIntervalSeconds, String... unsetVariables) throws IOException, InterruptedException {
        assertThat(SCRIPT).exists();
        File stdoutFile = fixtureDir.resolve("script.stdout").toFile();
        File stderrFile = fixtureDir.resolve("script.stderr").toFile();
        ProcessBuilder processBuilder = new ProcessBuilder("bash", SCRIPT.toAbsolutePath().toString());
        processBuilder.environment().put("PATH", binDir + File.pathSeparator + System.getenv("PATH"));
        processBuilder.environment().put("REALM", "EXAMPLE.COM");
        processBuilder.environment().put("DOMAIN", DOMAIN);
        processBuilder.environment().put("ADMIN_USER", "admin");
        processBuilder.environment().put("FPW", "secret");
        processBuilder.environment().put("TIMEOUT_SECONDS", String.valueOf(timeoutSeconds));
        processBuilder.environment().put("POLL_INTERVAL_SECONDS", String.valueOf(pollIntervalSeconds));
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

    private void writeMasters(String... hosts) throws IOException {
        String ldif = Arrays.stream(hosts)
                .map(host -> "dn: cn=" + host + ",cn=masters,cn=ipa,cn=etc,dc=example,dc=com\n")
                .collect(Collectors.joining("\n"));
        Files.writeString(fixtureDir.resolve("masters.ldif"), ldif, StandardCharsets.UTF_8);
    }

    private void writeLocal(String... agreements) throws IOException {
        Files.writeString(fixtureDir.resolve("local.ldif"), String.join("", agreements), StandardCharsets.UTF_8);
    }

    private void writePeer(String peerHost, String... agreements) throws IOException {
        Files.writeString(fixtureDir.resolve(peerHost + ".ldif"), String.join("", agreements), StandardCharsets.UTF_8);
    }

    private void removePeerFixture(String peerHost) throws IOException {
        Files.delete(fixtureDir.resolve(peerHost + ".ldif"));
    }

    private void markUnqueryable(String peerHost) throws IOException {
        Files.writeString(fixtureDir.resolve(peerHost + ".fail"), "", StandardCharsets.UTF_8);
    }

    private static String ok(Suffix suffix, String toHost) {
        return agreement(suffix, toHost, OK_STATUS, REAL_END, "FALSE");
    }

    private static String idle(Suffix suffix, String toHost) {
        return agreement(suffix, toHost, IDLE_STATUS, EPOCH_END, "FALSE");
    }

    private static String replica(Suffix suffix, String... ruvElements) {
        List<String> lines = new ArrayList<>();
        lines.add("dn: cn=replica,cn=" + suffix.escapedDn + ",cn=mapping tree,cn=config");
        lines.add("cn: replica");
        lines.add("nsDS5ReplicaRoot: " + suffix.dn);
        Arrays.stream(ruvElements).forEach(element -> lines.add("nsds50ruv: " + element));
        return String.join("\n", lines) + "\n\n";
    }

    private static String generation(String value) {
        return "{replicageneration} " + value;
    }

    private static String rid(int rid, String maxTime) {
        return "{replica " + rid + " ldap://ipaserver" + rid + "." + DOMAIN + ":389} " + csn(BASE_TIME, rid) + " " + csn(maxTime, rid);
    }

    private static String ridWithoutCsns(int rid) {
        return "{replica " + rid + " ldap://ipaserver" + rid + "." + DOMAIN + ":389}";
    }

    private static String ridWithoutUrl(int rid, String maxTime) {
        return "{replica " + rid + "} " + csn(BASE_TIME, rid) + " " + csn(maxTime, rid);
    }

    private static String ridWithTrailingToken(int rid, String maxTime) {
        return rid(rid, maxTime) + " " + BASE_TIME;
    }

    /** A 20 hex character CSN: time (8), sequence number (4), replica id (4), sub-sequence number (4). */
    private static String csn(String time, int rid) {
        return time + "0000" + String.format("%04x", rid) + "0000";
    }

    private static String agreement(Suffix suffix, String toHost, String status, String end, String inProgress) {
        List<String> lines = new ArrayList<>();
        lines.add("dn: cn=" + suffix.cnPrefix + toHost + ",cn=replica,cn=" + suffix.escapedDn + ",cn=mapping tree,cn=config");
        lines.add("cn: " + suffix.cnPrefix + toHost);
        lines.add("nsDS5ReplicaHost: " + toHost);
        lines.add("nsDS5ReplicaRoot: " + suffix.dn);
        lines.add("nsds5replicaLastUpdateStatus: " + status);
        lines.add("nsds5replicaLastUpdateEnd: " + end);
        lines.add("nsds5replicaUpdateInProgress: " + inProgress);
        return String.join("\n", lines) + "\n\n";
    }

    private enum Suffix {
        IPA("meTo", "dc=example,dc=com", "dc\\3Dexample\\2Cdc\\3Dcom"),
        CA("caTo", "o=ipaca", "o\\3Dipaca");

        private final String cnPrefix;

        private final String dn;

        private final String escapedDn;

        Suffix(String cnPrefix, String dn, String escapedDn) {
            this.cnPrefix = cnPrefix;
            this.dn = dn;
            this.escapedDn = escapedDn;
        }
    }

    private record ScriptResult(int exitCode, String stdout, String stderr) {
    }
}
