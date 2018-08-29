package com.sequenceiq.cloudbreak.template;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class TemplateParameterFilter {

    public Set<String> queryForDatalakeParameters(HandleBarModelKey key, List<String> blueprintParameters) {
        String prefix = String.format("%s.", key.modelKey());
        return blueprintParameters.stream()
                .filter(b -> b.startsWith(prefix))
                .map(b -> b.replaceFirst(prefix, ""))
                .collect(Collectors.toSet());
    }

    public Set<String> queryForCustomParameters(List<String> blueprintParameters) {
        String joinString = "^(" + String.join(".*|", HandleBarModelKey.modelKeys()) + ".*)";
        return blueprintParameters.stream()
                .filter(b -> !b.matches(joinString))
                .collect(Collectors.toSet());
    }
}
