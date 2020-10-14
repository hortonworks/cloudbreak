package com.sequenceiq.authorization.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class AuthorizationMessageUtils {

    public static final String INSUFFICIENT_RIGHTS = "You have insufficient rights to perform the following action(s): ";

    public static final String INSUFFICIENT_RIGHTS_TEMPLATE = "'%s' on a(n) '%s' type resource with resource identifier: '%s'";

    private AuthorizationMessageUtils() {
    }

    public static String formatTemplate(String right, String resourceCrn) {
        return INSUFFICIENT_RIGHTS + formatTemplateMessage(right, resourceCrn);
    }

    private static String formatTemplateMessage(String right, String resourceCrn) {
        return String.format(INSUFFICIENT_RIGHTS_TEMPLATE, right, extractResourceName(resourceCrn), resourceCrn);
    }

    private static String extractResourceName(String resourceCrn) {
        return Optional.ofNullable(resourceCrn)
                .map(Crn::fromString)
                .map(Crn::getResourceType)
                .map(Crn.ResourceType::getName)
                .orElse("unknown");
    }

    public static String formatTemplate(String right, Collection<String> resourceCrns) {
        return INSUFFICIENT_RIGHTS + resourceCrns.stream().map(crn -> formatTemplateMessage(right, crn)).collect(Collectors.joining(","));
    }

    public static String formatTemplate(List<AuthorizationProto.RightCheck> rightCheckList) {
        return INSUFFICIENT_RIGHTS + rightCheckList.stream()
                .map(rightCheck -> formatTemplateMessage(rightCheck.getRight(), rightCheck.getResource()))
                .collect(Collectors.joining(","));
    }
}
