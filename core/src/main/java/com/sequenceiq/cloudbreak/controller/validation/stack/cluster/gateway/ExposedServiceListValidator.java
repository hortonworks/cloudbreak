package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.Validator;

@Component
public class ExposedServiceListValidator implements Validator<List<String>> {

    @Override
    public ValidationResult validate(List<String> subject) {
        List<String> invalidKnoxServices = subject.stream()
                .filter(es -> !ExposedService.isKnoxExposed(es))
                .filter(es -> !ExposedService.ALL.getServiceName().equalsIgnoreCase(es))
                .collect(Collectors.toList());

        return ValidationResult.builder()
                .ifError(() -> !invalidKnoxServices.isEmpty(), "The following services are not supported Knox services: "
                        + String.join(", ", invalidKnoxServices))
                .build();
    }
}
