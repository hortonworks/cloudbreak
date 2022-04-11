package com.sequenceiq.cloudbreak.apiformat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opentest4j.AssertionFailedError;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class ApiFormatValidator {

    private final String modelPackage;

    private final Set<Class<?>> excludedClasses;

    private final Map<Class<?>, Set<ApiFormatRule>> excludedRulesByClasses;

    public ApiFormatValidator(String packageName, Set<Class<?>> excludedClasses, Map<Class<?>, Set<ApiFormatRule>> excludedRulesByClasses) {
        this.modelPackage = packageName;
        this.excludedClasses = excludedClasses;
        this.excludedRulesByClasses = excludedRulesByClasses;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void validate() {
        List<ApiFormatValidationResult> apiFormatErrors = getModelClasses(modelPackage)
                .stream()
                .filter(c -> !excludedClasses.contains(c))
                .map(this::validateFormattingRules)
                .filter(ApiFormatValidationResult::hasError)
                .collect(Collectors.toList());

        if (!apiFormatErrors.isEmpty()) {
            throw new AssertionFailedError("The following API model classes(" + apiFormatErrors.size() + ") have formatting issues:\n\n" +
                    apiFormatErrors
                            .stream()
                            .map(ApiFormatValidationResult::toString)
                            .collect(Collectors.joining("\n\n")));
        }
    }

    private ApiFormatValidationResult validateFormattingRules(Class<?> modelClass) {
        ApiFormatValidationResult formatValidation = new ApiFormatValidationResult(modelClass);
        for (ApiFormatRule apiFormatRule : ApiFormatRules.values()) {
            if (!excludedRulesByClasses.containsKey(modelClass) || !excludedRulesByClasses.get(modelClass).contains(apiFormatRule)) {
                Optional<String> error = apiFormatRule.apply(formatValidation.getValidatedClass());
                error.ifPresent(formatValidation::addError);
            }
        }
        return formatValidation;
    }

    private Set<Class<?>> getModelClasses(String packageName) {
        return new Reflections(packageName, new SubTypesScanner(false)).getSubTypesOf(Object.class);
    }

    public static class Builder {

        private String modelPackage;

        private Set<Class<?>> excludedClasses = new HashSet<>();

        private Map<Class<?>, Set<ApiFormatRule>> excludedRulesByClasses = new HashMap<>();

        public Builder modelPackage(String modelPackage) {
            this.modelPackage = modelPackage;
            return this;
        }

        public Builder excludedClasses(Class<?>... excludedClasses) {
            this.excludedClasses.addAll(Set.of(excludedClasses));
            return this;
        }

        public Builder excludeRules(Class<?> clazz, ApiFormatRule... rules) {
            excludedRulesByClasses.computeIfAbsent(clazz, i -> new HashSet<>()).addAll(Set.of(rules));
            return this;
        }

        public ApiFormatValidator build() {
            return new ApiFormatValidator(modelPackage, excludedClasses, excludedRulesByClasses);
        }
    }
}
