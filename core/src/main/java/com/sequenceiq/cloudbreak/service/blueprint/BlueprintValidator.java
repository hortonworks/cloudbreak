package com.sequenceiq.cloudbreak.service.blueprint;


import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;

@Component
public class BlueprintValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintValidator.class);

    private static final String MULTI_HOSTNAME_EXCEPTION_MESSAGE_FORMAT = "Host %s names must be unique! The following host %s names are invalid due to " +
            "their multiple occurrence: %s";

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    public boolean supports(Class<?> clazz) {
        return Blueprint.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Blueprint blueprint = (Blueprint) target;

        if (blueprint.getBlueprintText().isEmpty()) {
            errors.rejectValue("blueprintText", "empty", "The blueprint text is empty");
        }

        CmTemplateProcessor cm = cmTemplateProcessorFactory.get(blueprint.getBlueprintText());
        validateHostNames(cm, errors);
        validateRoleType(cm, errors);
    }

    private void validateRoleType(CmTemplateProcessor cm, Errors errors) {
        if (!cm.getServiceComponentsByHostGroup().entrySet()
                .stream()
                .map(entry -> entry.getValue())
                .flatMap(Collection::stream)
                .allMatch(component -> component.getComponent() != null)) {
            errors.rejectValue("blueprintText", "roleType", "Role type is mandatory in role config");
        }
    }

    private void validateHostNames(BlueprintTextProcessor blueprintTextProcessor, Errors errors) {
        LOGGER.debug("Validating CM template host names...");
        List<String> hostTemplateNames = blueprintTextProcessor.getHostTemplateNames();
        if (hostNamesAreNotUnique(hostTemplateNames)) {
            String nonUniqueHostTemplateNames = String.join(", ", findHostTemplateNameDuplicates(hostTemplateNames));
            String hostSectionIdentifier = blueprintTextProcessor.getHostGroupPropertyIdentifier();
            String message = String.format(MULTI_HOSTNAME_EXCEPTION_MESSAGE_FORMAT, hostSectionIdentifier, hostSectionIdentifier, nonUniqueHostTemplateNames);
            errors.rejectValue("blueprintText", "hostnames", message);
        }
    }

    private boolean hostNamesAreNotUnique(List<String> hostGroupNames) {
        return new HashSet<>(hostGroupNames).size() != hostGroupNames.size();
    }

    private Set<String> findHostTemplateNameDuplicates(List<String> hostTemplateNames) {
        Set<String> temp = new LinkedHashSet<>();
        return hostTemplateNames.stream().filter(hostTemplateName -> !temp.add(hostTemplateName)).collect(Collectors.toSet());
    }

}
