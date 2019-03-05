package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Component
public class RecipeExecutionFailureCollector {

    private static final Pattern PHASE_PATTERN = Pattern.compile(".*/opt/scripts/recipe-runner\\.sh ([a-zA-Z0-9-]+) (([a-zA-Z0-9-]+)).*", Pattern.DOTALL);

    public boolean canProcessExecutionFailure(CloudbreakOrchestratorException exception) {
        return !getNodesWithErrors(exception).isEmpty() && exception.getMessage().contains("/opt/scripts/recipe-runner.sh ");
    }

    public List<RecipeFailure> collectErrors(CloudbreakOrchestratorException exception) {
        Multimap<String, String> nodesWithErrors = getNodesWithErrors(exception);

        if (nodesWithErrors.isEmpty()) {
            throw new CloudbreakServiceException("Failed to collect recipe execution failures. Cause exception contains no information.", exception);
        }

        List<RecipeFailure> failures = nodesWithErrors.asMap().entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(v -> new SimpleImmutableEntry<>(e.getKey(), v))
                        .map(failure -> new RecipeFailure(failure.getKey(), getRecipePhase(failure.getValue()), getFailedRecipeName(failure.getValue()))))
                .filter(failure -> !failure.getRecipeName().isEmpty() && !failure.getPhase().isEmpty())
                .collect(Collectors.toList());

        return failures;
    }

    private Multimap<String, String> getNodesWithErrors(CloudbreakOrchestratorException exception) {
        Throwable current = exception;
        Multimap<String, String> errors = exception.getNodesWithErrors();
        while (current != null && errors.isEmpty()) {
            if (current instanceof CloudbreakOrchestratorException) {
                errors = ((CloudbreakOrchestratorException) current).getNodesWithErrors();
            }
            current = current.getCause();
        }
        return errors;
    }

    protected String getFailedRecipeName(String message) {
        Matcher matcher = PHASE_PATTERN.matcher(message);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return "";
    }

    protected String getRecipePhase(String message) {
        Matcher matcher = PHASE_PATTERN.matcher(message);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    public Optional<InstanceMetaData> getInstanceMetadataByHost(Set<InstanceMetaData> instanceMetaData, String host) {
        return instanceMetaData.stream().filter(metadata -> metadata.getDiscoveryFQDN().equals(host)).findFirst();
    }

    public static class RecipeFailure {

        private final String host;

        private final String phase;

        private final String recipeName;

        public RecipeFailure(String host, String phase, String recipeName) {
            this.host = host;
            this.phase = phase;
            this.recipeName = recipeName;
        }

        public String getHost() {
            return host;
        }

        public String getPhase() {
            return phase;
        }

        public String getRecipeName() {
            return recipeName;
        }
    }
}
