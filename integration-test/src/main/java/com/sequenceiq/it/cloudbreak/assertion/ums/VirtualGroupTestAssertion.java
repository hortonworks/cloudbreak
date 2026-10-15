package com.sequenceiq.it.cloudbreak.assertion.ums;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class VirtualGroupTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualGroupTestAssertion.class);

    private VirtualGroupTestAssertion() {
    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validateAdminVirtualGroupMembership(FreeIpaTestClient freeIpaTestClient, Map<UmsVirtualGroupRight,
            String> environmentVirtualGroups, Set<String> adminUsers, boolean expectedPresence) {
        return (testContext, freeIpaTestDto, freeIpaClient) -> {
            List<String> adminGroups = environmentVirtualGroups
                    .entrySet().stream()
                    .filter(group -> group.getKey().name().contains("ADMIN"))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());
            LOGGER.info(" Admin groups are present [{}] at environment '{}' ", adminGroups, freeIpaTestDto.getResponse().getEnvironmentCrn());
            adminGroups.forEach(adminGroup -> freeIpaTestClient.findUsersInGroup(adminUsers, adminGroup, expectedPresence));

            return freeIpaTestDto;
        };
    }

    public static Assertion<FreeIpaTestDto, FreeIpaClient> validateUserVirtualGroupMembership(FreeIpaTestClient freeIpaTestClient, Map<UmsVirtualGroupRight,
            String> environmentVirtualGroups, Set<String> environmentUsers, boolean expectedPresence) {
        return (testContext, freeIpaTestDto, freeIpaClient) -> {
            List<String> userGroups = environmentVirtualGroups
                .entrySet().stream()
                .filter(group -> group.getKey().name().contains("ACCESS"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
            LOGGER.info(" User groups are present [{}] at environment '{}' ", userGroups, freeIpaTestDto.getResponse().getEnvironmentCrn());
            userGroups.forEach(userGroup -> freeIpaTestClient.findUsersInGroup(environmentUsers, userGroup, expectedPresence));

            return freeIpaTestDto;
        };
    }
}
