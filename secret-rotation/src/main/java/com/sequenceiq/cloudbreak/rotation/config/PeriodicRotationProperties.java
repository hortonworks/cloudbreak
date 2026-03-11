package com.sequenceiq.cloudbreak.rotation.config;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.Min;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.sequenceiq.cloudbreak.rotation.SecretType;

@Validated
@Component
@ConfigurationProperties(prefix = "secretrotation.periodic")
public class PeriodicRotationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicRotationProperties.class);

    // 24 * 60 = daily
    private static final int DEFAULT_SCHEDULE_INTERVAL_MINUTES = 1440;

    private boolean enabled = true;

    /**
     * Frequency of the periodic rotation job execution, in minutes.
     * Defaults to daily (1440). Must be >= 1.
     */
    @Min(1)
    private int scheduleIntervalMinutes = DEFAULT_SCHEDULE_INTERVAL_MINUTES;

    /**
     * Key: SecretType enum name (e.g., FREEIPA_ADMIN_PASSWORD). Value: rotation interval.
     * Use Spring duration formats, e.g. "30d", "12h", "15m".
     * Null, zero or negative disables periodic rotation for the secret.
     */
    private Map<String, Duration> perSecret = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getScheduleIntervalMinutes() {
        return scheduleIntervalMinutes;
    }

    public void setScheduleIntervalMinutes(int scheduleIntervalMinutes) {
        this.scheduleIntervalMinutes = scheduleIntervalMinutes;
    }

    public Map<String, Duration> getPerSecret() {
        return perSecret;
    }

    public void setPerSecret(Map<String, Duration> perSecret) {
        this.perSecret = Optional.ofNullable(perSecret).orElseGet(LinkedHashMap::new);
    }

    /**
     * Resolve configured intervals to the provided set of enabled SecretType enums.
     * - Case-insensitive name match against enum names.
     * - Null, zero or negative intervals are treated as disabled and omitted.
     * - Unknown names are ignored.
     */
    public Map<SecretType, Duration> resolveIntervalsToSecretTypes(Collection<SecretType> enabledSecretTypes) {
        if (isEmptyInput(enabledSecretTypes, perSecret)) {
            return Collections.emptyMap();
        }
        Map<String, SecretType> nameToType = new LinkedHashMap<>();
        enabledSecretTypes.forEach(st ->
            nameToType.put(st.value().toUpperCase(Locale.ROOT), st)
        );

        Map<SecretType, Duration> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Duration> entry : perSecret.entrySet()) {
            String key = entry.getKey();
            Duration interval = entry.getValue();
            if (isInvalidEntry(key, interval)) {
                LOGGER.info("Invalid secret {} with interval {}", key, interval);
                continue;
            }
            SecretType secretType = nameToType.get(key.toUpperCase(Locale.ROOT));
            if (secretType != null) {
                resolved.put(secretType, interval);
            }
        }
        return resolved;
    }

    private boolean isEmptyInput(Collection<SecretType> enabledSecretTypes, Map<String, Duration> perSecret) {
        return enabledSecretTypes == null || enabledSecretTypes.isEmpty()
                || perSecret == null || perSecret.isEmpty();
    }

    private boolean isInvalidEntry(String key, Duration interval) {
        return key == null || interval == null || interval.isZero() || interval.isNegative();
    }

    public Optional<Duration> getIntervalFor(SecretType secretType) {
        if (secretType == null || perSecret == null) {
            return Optional.empty();
        }
        Duration interval = perSecret.get(secretType.value());
        if (interval == null) {
            // try case-insensitive fallback
            interval = perSecret.entrySet().stream()
                    .filter(e -> secretType.value().equalsIgnoreCase(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return Optional.empty();
        }
        return Optional.of(interval);
    }
}

