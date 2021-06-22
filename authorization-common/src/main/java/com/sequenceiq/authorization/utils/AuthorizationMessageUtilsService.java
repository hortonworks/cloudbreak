package com.sequenceiq.authorization.utils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.authorization.service.ResourceNameFactoryService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Service
public class AuthorizationMessageUtilsService {

    public static final String INSUFFICIENT_RIGHTS = "You have insufficient rights to perform the following action(s): ";

    public static final String INSUFFICIENT_RIGHTS_TEMPLATE = "'%s' on a(n) '%s' type resource with resource identifier: [%s]";

    private ResourceNameFactoryService resourceNameFactoryService;

    @Inject
    public AuthorizationMessageUtilsService(ResourceNameFactoryService resourceNameFactoryService) {
        this.resourceNameFactoryService = resourceNameFactoryService;
    }

    public String formatTemplate(String right, String resourceCrn) {
        return INSUFFICIENT_RIGHTS + formatTemplateMessage(right, resourceCrn);
    }

    private String formatTemplateMessage(String right, String resourceCrn) {
        String identifiers = "'account'";
        if (resourceCrn != null) {
            String resourceNameFormatted = Optional.ofNullable(resourceNameFactoryService.getNames(List.of(resourceCrn)))
                    .flatMap(names -> names.getOrDefault(resourceCrn, Optional.empty()))
                    .map(name -> String.format("Name: '%s'", name))
                    .orElse("");
            String resourceCrnFormatted = String.format("Crn: '%s'", resourceCrn);
            identifiers = Stream.of(resourceNameFormatted, resourceCrnFormatted)
                    .filter(part -> !part.isEmpty())
                    .collect(Collectors.joining(", "));
        }
        return String.format(INSUFFICIENT_RIGHTS_TEMPLATE, right, extractResourceType(resourceCrn), identifiers);
    }

    private String extractResourceType(String resourceCrn) {
        return Optional.ofNullable(resourceCrn)
                .map(Crn::fromString)
                .map(Crn::getResourceType)
                .map(Crn.ResourceType::getName)
                .orElse("unknown");
    }

    public String formatTemplate(String right, Collection<String> resourceCrns) {
        return INSUFFICIENT_RIGHTS + resourceCrns.stream().map(crn -> formatTemplateMessage(right, crn)).collect(Collectors.joining(","));
    }

    public String formatTemplate(List<AuthorizationProto.RightCheck> rightCheckList) {
        return INSUFFICIENT_RIGHTS + rightCheckList.stream()
                .map(rightCheck -> formatTemplateMessage(rightCheck.getRight(), rightCheck.getResource()))
                .collect(Collectors.joining(","));
    }
}
