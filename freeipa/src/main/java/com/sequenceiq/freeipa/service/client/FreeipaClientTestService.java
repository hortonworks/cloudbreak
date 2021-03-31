package com.sequenceiq.freeipa.service.client;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class FreeipaClientTestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaClientTestService.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public String userShow(Long id, String name) {
        FreeIpaClient freeIpaClient;
        try {
            freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(id);
        } catch (Exception e) {
            LOGGER.error("Error creating FreeIpaClient", e);
            return "FAILED TO CREATE CLIENT";
        }

        try {
            User user = freeIpaClient.userShow(name);
            LOGGER.info("Groups: {}", user.getMemberOfGroup());
            LOGGER.info("Success: {}", user);
        } catch (Exception e) {
            LOGGER.error("Error showing user {}", name, e);
            return "FAILED TO SHOW USER";
        }
        return "END";
    }

    public Boolean checkUsers(String environmentCrn, Set<String> requestedUsers) {
        FreeIpaClient freeIpaClient = getClientByEnvironmentCrn(environmentCrn);
        try {
            LOGGER.info("Checking for users [{}] in environment {}", Joiner.on(",").join(requestedUsers), environmentCrn);
            Set<String> freeipaUsers = freeIpaClient.userFindAll().stream().map(User::getUid).collect(Collectors.toSet());
            LOGGER.debug("Users in freeipa: [{}]", Joiner.on(",").join(freeipaUsers));
            return freeipaUsers.containsAll(requestedUsers);
        } catch (FreeIpaClientException e) {
            LOGGER.error("Find user FreeIPA call failed!", e);
            throw new InternalServerErrorException("Find user FreeIPA call failed!");
        }
    }

    public Boolean checkGroups(String environmentCrn, Set<String> requestGroups) {
        FreeIpaClient freeIpaClient = getClientByEnvironmentCrn(environmentCrn);
        try {
            LOGGER.info("Checking for groups [{}] in environment {}", Joiner.on(",").join(requestGroups), environmentCrn);
            Set<String> freeipaGroups = freeIpaClient.groupFindAll().stream().map(Group::getCn).collect(Collectors.toSet());
            LOGGER.debug("Groups in freeipa: [{}]", Joiner.on(",").join(freeipaGroups));
            return freeipaGroups.containsAll(requestGroups);
        } catch (FreeIpaClientException e) {
            LOGGER.error("Find group FreeIPA call failed!", e);
            throw new InternalServerErrorException("Find group FreeIPA call failed!");
        }
    }

    public Boolean checkUsersInGroup(String environmentCrn, Set<String> requestedUsers, String requestedGroup) {
        FreeIpaClient freeIpaClient = getClientByEnvironmentCrn(environmentCrn);
        try {
            LOGGER.info("Checking if group [{}] has users [{}] in environment {}", requestedGroup,
                    Joiner.on(",").join(requestedUsers), environmentCrn);
            Set<Group> freeipaGroups = freeIpaClient.groupFindAll();
            LOGGER.debug("Groups in freeipa: {}", Joiner.on(",").join(freeipaGroups.stream().map(Group::getCn).collect(Collectors.toSet())));
            Optional<Group> freeipaGroup = freeipaGroups.stream().filter(ipaGroup ->
                    StringUtils.equals(ipaGroup.getCn(), requestedGroup)).findFirst();
            if (freeipaGroup.isPresent()) {
                LOGGER.debug("Group [{}] found in freeipa, it has users [{}].", freeipaGroup.get().getCn(), freeipaGroup.get().getMemberUser());
            }
            return freeipaGroup.isPresent() && freeipaGroup.get().getMemberUser().containsAll(requestedUsers);
        } catch (FreeIpaClientException e) {
            LOGGER.error("Find group FreeIPA call failed!", e);
            throw new InternalServerErrorException("Find group FreeIPA call failed!");
        }
    }

    private FreeIpaClient getClientByEnvironmentCrn(String environmentCrn) {
        try {
            return freeIpaClientFactory.getFreeIpaClientByAccountAndEnvironment(
                    environmentCrn, ThreadBasedUserCrnProvider.getAccountId());
        } catch (FreeIpaClientException e) {
            LOGGER.error("FreeIPA client cannot be created!", e);
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }
}
