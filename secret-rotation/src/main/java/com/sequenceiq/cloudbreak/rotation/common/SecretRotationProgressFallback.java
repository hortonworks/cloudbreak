package com.sequenceiq.cloudbreak.rotation.common;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.request.RotationSource;
import com.sequenceiq.cloudbreak.rotation.request.StepProgressResponse;

public class SecretRotationProgressFallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationProgressFallback.class);

    private SecretRotationProgressFallback() {
    }

    public static Optional<StepProgressResponse> findFirstProgress(Map<RotationSource, SecretType> pollingTypes,
            BiFunction<RotationSource, SecretType, StepProgressResponse> progressQuery) {
        for (Map.Entry<RotationSource, SecretType> pollingType : pollingTypes.entrySet()) {
            RotationSource source = pollingType.getKey();
            SecretType secretType = pollingType.getValue();
            try {
                StepProgressResponse response = progressQuery.apply(source, secretType);
                if (response != null) {
                    return Optional.of(response);
                }
            } catch (NotFoundException | jakarta.ws.rs.NotFoundException e) {
                LOGGER.debug("No rotation progress found in {} for secret type '{}'.", source, secretType.value());
            } catch (Exception e) {
                LOGGER.warn("Failed to retrieve rotation progress from {} for secret type '{}'.", source, secretType.value(), e);
            }
        }
        return Optional.empty();
    }
}
