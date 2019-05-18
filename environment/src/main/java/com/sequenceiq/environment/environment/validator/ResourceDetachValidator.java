package com.sequenceiq.environment.environment.validator;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.ValidationResult;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Component
public class ResourceDetachValidator {

    public ValidationResult validate(EnvironmentAwareResource resource, Map<EnvironmentView, Set<String>> envsToClusters) {
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
