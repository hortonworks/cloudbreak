package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.Validator;

@Component
public class ExposedServiceListValidator implements Validator<List<String>> {

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Override
    public ValidationResult validate(List<String> subject) {
        List<String> invalidKnoxServices = subject.stream()
                .filter(es -> !exposedServiceCollector.isKnoxExposed(es))
                .filter(es -> !ExposedServiceCollector.ALL.equalsIgnoreCase(es))
                .collect(Collectors.toList());

        return ValidationResult.builder()
                .ifError(() -> !invalidKnoxServices.isEmpty(), "The following services are not supported Knox services: "
                        + String.join(", ", invalidKnoxServices))
                .build();
    }
}
