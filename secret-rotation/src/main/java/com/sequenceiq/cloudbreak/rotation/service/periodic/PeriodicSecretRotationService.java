package com.sequenceiq.cloudbreak.rotation.service.periodic;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;

/**
 * Interface for periodic secret rotation.
 * Uses resource CRN only—resource resolution is handled by implementations.
 */
public interface PeriodicSecretRotationService {

    List<JobResource> listJobResources();

    /**
     * Returns MDC context info for the resource, if available.
     */
    default Optional<MdcContextInfoProvider> getMdcContextInfoProvider(String resourceCrn) {
        return Optional.empty();
    }

    boolean isSchedulable(String resourceCrn);

    List<String> listRotatableSecretNames(String resourceCrn);

    List<SecretType> enabledSecretTypes();

    void triggerRotation(String resourceCrn, List<String> dueSecretNames);

    Instant getResourceCreationDate(String resourceCrn);
}

