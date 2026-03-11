package com.sequenceiq.cloudbreak.rotation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;

class PeriodicRotationPropertiesTest {

    @Test
    void resolveIntervalsFiltersUnknownAndInvalidAndMatchesCaseInsensitive() {
        PeriodicRotationProperties props = new PeriodicRotationProperties();
        // Secret names that cover case-insensitive, unknown.
        // Secrets with zero & negative duration
        props.setPerSecret(Map.of(
                "TEST", Duration.ofDays(30),
                "test_2", Duration.ofHours(12),
                "UNKNOWN", Duration.ofDays(10),
                "TEST_3", Duration.ZERO,
                "TEST_4", Duration.ofDays(-1)
        ));
        List<SecretType> enabled = List.of(TestSecretType.values());

        Map<SecretType, Duration> resolved = props.resolveIntervalsToSecretTypes(enabled);

        assertThat(resolved)
                .hasSize(2)
                .containsEntry(TestSecretType.TEST, Duration.ofDays(30))
                .containsEntry(TestSecretType.TEST_2, Duration.ofHours(12))
                .doesNotContainKey(TestSecretType.TEST_3)
                .doesNotContainKey(TestSecretType.TEST_4);
    }

    @Test
    void resolveIntervalsEmptyWhenNoEnabledOrNoConfig() {
        PeriodicRotationProperties props = new PeriodicRotationProperties();
        assertThat(props.resolveIntervalsToSecretTypes(List.of())).isEmpty();
        props.setPerSecret(Map.of());
        assertThat(props.resolveIntervalsToSecretTypes(List.of(TestSecretType.TEST))).isEmpty();
    }

    @Test
    void getIntervalForHonorsCaseInsensitiveAndFiltersInvalid() {
        PeriodicRotationProperties props = new PeriodicRotationProperties();
        props.setPerSecret(Map.of(
                "test", Duration.ofMinutes(15),
                "TEST_3", Duration.ZERO,
                "TEST_4", Duration.ofSeconds(-1)
        ));

        assertThat(props.getIntervalFor(TestSecretType.TEST)).contains(Duration.ofMinutes(15));
        assertThat(props.getIntervalFor(TestSecretType.TEST_3)).isEmpty();
        assertThat(props.getIntervalFor(TestSecretType.TEST_4)).isEmpty();
        assertThat(props.getIntervalFor(null)).isEmpty();
    }
}

