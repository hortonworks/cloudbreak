package com.sequenceiq.cloudbreak.rotation.job;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties;
import com.sequenceiq.cloudbreak.rotation.service.history.SecretRotationHistoryService;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;

/**
 * Periodic secret rotation job that delegates to a PeriodicSecretRotationService.
 * Uses only resource CRN—resource resolution is handled by the service.
 */
@DisallowConcurrentExecution
@Component
@ConditionalOnBean(PeriodicSecretRotationService.class)
public class PeriodicSecretRotationJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicSecretRotationJob.class);

    @Inject
    private PeriodicRotationProperties periodicRotationProperties;

    @Inject
    private SecretRotationHistoryService secretRotationHistoryService;

    @Inject
    private PeriodicSecretRotationService rotationService;

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return rotationService.getMdcContextInfoProvider(getRemoteResourceCrn());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        super.executeTracedJob(context);
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws JobExecutionException {
        if (!periodicRotationProperties.isEnabled()) {
            LOGGER.debug("Periodic secret rotation disabled by service, skipping run.");
            return;
        }
        Map<SecretType, Duration> configuredIntervals =
            periodicRotationProperties.resolveIntervalsToSecretTypes(rotationService.enabledSecretTypes());
        if (configuredIntervals.isEmpty()) {
            LOGGER.debug("No periodic per-secret intervals resolved; skipping run.");
            return;
        }
        String resourceCrn = getRemoteResourceCrn();
        LOGGER.debug("Resolved periodic intervals: {} for resource '{}'", configuredIntervals, resourceCrn);

        if (!rotationService.isSchedulable(resourceCrn)) {
            LOGGER.debug("Resource '{}' is not schedulable, skipping.", resourceCrn);
            return;
        }
        List<String> rotatableSecretNames = rotationService.listRotatableSecretNames(resourceCrn);
        List<SecretType> rotatableConfigured = rotatableSecretNames.stream()
            .map(name -> mapToSecretTypeOrNull(name, rotationService.enabledSecretTypes()))
            .filter(Objects::nonNull)
            .filter(configuredIntervals::containsKey)
            .toList();

        List<String> dueSecretNames = rotatableConfigured.stream()
            .filter(secretType -> secretRotationHistoryService.checkIfRotationDue(resourceCrn, secretType,
                configuredIntervals.get(secretType), rotationService.getResourceCreationDate(resourceCrn)))
            .map(SecretType::value)
            .toList();

        if (!dueSecretNames.isEmpty()) {
            LOGGER.info("Triggering periodic rotation with resource crn '{}' secrets {}",
                resourceCrn, dueSecretNames);
            rotationService.triggerRotation(resourceCrn, dueSecretNames);
        } else {
            LOGGER.debug("No secrets due for periodic rotation for resource '{}'", resourceCrn);
        }
    }

    private SecretType mapToSecretTypeOrNull(String name, List<SecretType> enabled) {
        for (SecretType secretType : enabled) {
            if (secretType.value().equalsIgnoreCase(name)) {
                return secretType;
            }
        }
        return null;
    }
}

