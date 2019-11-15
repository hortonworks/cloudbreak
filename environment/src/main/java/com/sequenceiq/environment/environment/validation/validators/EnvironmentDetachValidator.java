package com.sequenceiq.environment.environment.validation.validators;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Component
public class EnvironmentDetachValidator {

    public ValidationResult validateForEnvironment(Environment environment, Map<? extends EnvironmentAwareResource, Set<String>> resourcesToClusters) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        resourcesToClusters.forEach((resource, clusters) -> {
            if (!clusters.isEmpty()) {
                String message = String.format("%s '%s' cannot be detached from environment '%s' because it is used by the following cluster(s): [%s]",
                        resource.getClass().getName(), resource.getName(), environment.getName(),
                        String.join(", ", clusters));
                resultBuilder.error(message);
            }
        });
        return resultBuilder.build();
    }

    public ValidationResult validateForResource(EnvironmentAwareResource resource, Map<EnvironmentView, Set<String>> envsToClusters) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        envsToClusters.forEach((environmentView, clusters) -> {
            if (!clusters.isEmpty()) {
                String message = String.format("%s '%s' cannot be detached from environment '%s' because it is used by the following cluster(s): [%s]",
                        resource.getClass().getName(), resource.getName(), environmentView.getName(),
                        String.join(", ", clusters));
                resultBuilder.error(message);
            }
        });
        return resultBuilder.build();
    }
}
