package com.sequenceiq.cloudbreak.util;

import static java.lang.System.lineSeparator;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.reflections.Reflections;
import org.reflections.scanners.MemberUsageScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class ConstraintValidationModificationChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintValidationModificationChecker.class);

    public void check() {
        Reflections reflections = new Reflections("com.sequenceiq",
                new MemberUsageScanner());

        String validatorUtilCName = ValidatorUtil.class.getName();
        Set<String> notTestAndValidatorUtilInvokers = reflections.getStore().get("MemberUsageScanner")
                .entrySet()
                .stream()
                .filter(stringSetEntry -> stringSetEntry.getKey().startsWith(ConstraintValidatorContext.ConstraintViolationBuilder.class.getName())
                        || stringSetEntry.getKey().startsWith(HibernateConstraintValidatorContext.class.getName()))
                .flatMap(usageEntry -> usageEntry.getValue().stream())
                .filter(usageDetails -> !usageDetails.startsWith(validatorUtilCName) && !usageDetails.contains("Test."))
                .collect(Collectors.toSet());

        if (!notTestAndValidatorUtilInvokers.isEmpty()) {
            String joinedInvocations = String.join(lineSeparator(), notTestAndValidatorUtilInvokers);
            String usage = String.format("The usage of ConstraintViolationBuilder.class or HibernateConstraintValidatorContext.class is only allowed in "
                    + "%s! Invalid usages: %s%s", validatorUtilCName, lineSeparator(), joinedInvocations);
            LOGGER.error(usage);
            throw new IllegalStateException(usage);
        }
    }
}
