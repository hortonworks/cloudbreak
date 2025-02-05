package com.sequenceiq.cloudbreak.cmtemplate.validation;

import java.util.function.Predicate;

public record ServiceRoleRestriction(String service, String role, Predicate<Integer> restriction, String message) {
}
