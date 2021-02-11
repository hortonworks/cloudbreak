package com.sequenceiq.cloudbreak.validation;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Service
public class HueWorkaroundValidatorService {

    private static final int MAX_ENDPOINT_LENGTH = 63;

    private static final int MAX_STACK_NAME_LENGTH = 20;

    private static final int MAX_HOST_GROUP_NAME = 6;

    public void validateForStackRequest(Set<String> hueHostGroups, String stackName) {
        if (isHuePresented(hueHostGroups)) {
            validateLimitedStackNameLength(stackName);
            // We need to validate the request for the existing blueprints which were added before this change
            validateLimitedHostgroupNameLength(hueHostGroups);
        }
    }

    public void validateForBlueprintRequest(Set<String> hueHostGroups) {
        if (isHuePresented(hueHostGroups)) {
            validateLimitedHostgroupNameLength(hueHostGroups);
        }
    }

    public void validateForEnvironmentDomainName(Set<String> hueHostGroups, String endpointName) {
        if (isHuePresented(hueHostGroups)) {
            if (tooLongEnpointName(endpointName)) {
                throw new BadRequestException(String.format("Your Data Hub contains Hue. Hue does not support CDP Data "
                        + "Hubs where the fully qualified domain name of a VM is longer than 63 characters. "
                        + "Generated hostname: %s. Please retry creating Data Hub using a Data Hub name that is shorter than "
                        + "20 characters and a hostgroup name that is shorter than 6 characters.", endpointName));
            }
        }
    }

    private boolean isHuePresented(Set<String> hueHostGroups) {
        return !hueHostGroups.isEmpty();
    }

    private void validateLimitedHostgroupNameLength(Set<String> hueHostGroups) {
        if (isHuePresented(hueHostGroups)) {
            Set<String> hostGroupsWhichAreLongerThanSixCharacter = hueHostGroups
                    .stream()
                    .filter(e -> e.length() > MAX_HOST_GROUP_NAME)
                    .collect(Collectors.toSet());
            if (!hostGroupsWhichAreLongerThanSixCharacter.isEmpty()) {
                throw new BadRequestException(String.format("Hue does not support CDP Data Hubs where hostgroup name is longer than 6 characters. "
                                + "Please retry creating the template by shortening the hostgroup name: %s  under 6 characters.",
                        String.join(", ", hostGroupsWhichAreLongerThanSixCharacter)));
            }
        }
    }

    private void validateLimitedStackNameLength(String name) {
        if (name.length() > MAX_STACK_NAME_LENGTH) {
            throw new BadRequestException("Your Data Hub contains Hue. Hue does not support CDP Data Hubs where Data Hub name is longer than 20 characters. "
                    + "Please retry creating Data Hub using a Data Hub name that is shorter than 20 characters.");
        }
    }

    private boolean tooLongEnpointName(String enpointName) {
        if (enpointName.length() > MAX_ENDPOINT_LENGTH) {
            return true;
        }
        return false;
    }
}
