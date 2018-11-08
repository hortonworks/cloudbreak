package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.EnvironmentAwareResource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class EnvironmentDetachValidator {

    public ValidationResult validate(Environment environment, Map<? extends EnvironmentAwareResource, Set<Cluster>> resourcesToClusters) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        resourcesToClusters.forEach((resource, clusters) -> {
            if (!clusters.isEmpty()) {
                String message = String.format("%s '%s' cannot be detached from environment '%s' because it is used by the following cluster(s): [%s]",
                        resource.getResource().getReadableName(), resource.getName(), environment.getName(),
                        clusters.stream().map(Cluster::getName).collect(Collectors.joining(", ")));
                resultBuilder.error(message);
            }
        });
        return resultBuilder.build();
    }
}
