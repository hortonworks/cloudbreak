package com.sequenceiq.maintenance.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;

class MaintenanceTaskExecutionRefTest {

    @Test
    void parseResolvesSubmitterServiceAndExecutePath() {
        MaintenanceTaskExecutionRef ref = MaintenanceTaskExecutionRef.parse(new Json(Map.of(
                "submitter_service", "datalake",
                "execute_path", "/internal/maintenance-tasks/execute")), "core");

        assertThat(ref.submitterService()).isEqualTo("datalake");
        assertThat(ref.executePath()).isEqualTo("/internal/maintenance-tasks/execute");
    }

    @Test
    void parseUsesTaskSubmitterServiceWhenOmittedInExecutionRef() {
        MaintenanceTaskExecutionRef ref = MaintenanceTaskExecutionRef.parse(new Json(Map.of(
                "execute_path", "/internal/foo")), "freeipa");

        assertThat(ref.submitterService()).isEqualTo("freeipa");
        assertThat(ref.executePath()).isEqualTo("/internal/foo");
    }

    @Test
    void parseRejectsMissingExecutePath() {
        assertThatThrownBy(() -> MaintenanceTaskExecutionRef.parse(new Json(Map.of(
                "submitter_service", "datalake")), "datalake"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("execute_path");
    }
}
