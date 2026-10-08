package com.sequenceiq.cloudbreak.rotation.maintenance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

class MaintenanceWindowSecretRotationSupportTest {

    @Test
    void resolveSecretNameUsesWorkItemIdWhenPayloadAbsent() {
        assertThat(MaintenanceWindowSecretRotationSupport.resolveSecretName("SALT_PASSWORD", null))
                .isEqualTo("SALT_PASSWORD");
    }

    @Test
    void resolveSecretNameRejectsConflictingPayload() {
        assertThatThrownBy(() -> MaintenanceWindowSecretRotationSupport.resolveSecretName(
                "SALT_PASSWORD", Map.of(MaintenanceWindowSecretRotationSupport.PAYLOAD_SECRET_NAMES, List.of("OTHER"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must contain only");
    }

    @Test
    void maintenanceWindowAdditionalPropertiesSetsPeriodicRotationFlags() {
        assertThat(MaintenanceWindowSecretRotationSupport.maintenanceWindowAdditionalProperties("acc-1", 10L, 20L))
                .containsEntry("ignore-prevalidate-errors", "true")
                .containsEntry("maintenance-window-account-id", "acc-1")
                .containsEntry("maintenance-window-task-id", "10")
                .containsEntry("maintenance-window-run-id", "20");
    }
}
