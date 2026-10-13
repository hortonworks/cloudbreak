package com.sequenceiq.freeipa.salt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * VerifyReplicationCleanupService finds the scripts' timeout lines (printed to stderr) in the salt run output. That only works
 * while the salt states merge stderr into stdout with "2>&1" and keep the script's exit code through the tee pipe.
 */
class FreeIpaReplicationSaltStateContractTest {

    private static final Path SALT_DIR = Path.of("src/main/resources/freeipa-salt/salt/freeipa");

    @ParameterizedTest
    @CsvSource({
            "verify-replication-cleanup.sls, verify_replication_cleanup, freeipa_check_replication_cleanup.sh",
            "verify-replication-convergence.sls, verify_replication_convergence, freeipa_verify_replica_health.sh"
    })
    void testStateRunsScriptWithStderrMergedAndExitCodePreserved(String stateFile, String stateId, String script) throws IOException {
        String command = commandOf(stateFile, stateId);

        assertThat(command).startsWith("/opt/salt/scripts/" + script + " 2>&1 | tee -a ");
        assertThat(command).endsWith("&& exit ${PIPESTATUS[0]}");
    }

    private static String commandOf(String stateFile, String stateId) throws IOException {
        Path path = SALT_DIR.resolve(stateFile);
        assertThat(path).exists();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        int stateIndex = lines.indexOf(stateId + ":");
        assertThat(stateIndex).as("state %s in %s", stateId, stateFile).isNotNegative();
        return lines.stream()
                .skip(stateIndex + 1)
                .takeWhile(line -> line.startsWith(" "))
                .map(String::strip)
                .filter(line -> line.startsWith("- name: "))
                .map(line -> line.substring("- name: ".length()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No cmd.run name line found for state " + stateId + " in " + stateFile));
    }
}
