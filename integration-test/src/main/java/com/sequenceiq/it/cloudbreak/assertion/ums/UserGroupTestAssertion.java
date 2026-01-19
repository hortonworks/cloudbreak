package com.sequenceiq.it.cloudbreak.assertion.ums;

import static java.lang.String.format;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

import io.grpc.StatusRuntimeException;

public class UserGroupTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserGroupTestAssertion.class);

    private UserGroupTestAssertion() {
    }

    public static Assertion<UmsGroupTestDto, UmsClient> validateUserGroupMembership(CloudbreakUser groupMember, String groupName, boolean expectedPresence) {
        return (testContext, umsGroupTestDto, umsClient) -> {
            String accountId = testContext.getActingUserCrn().getAccountId();

            List<String> groupMembers = umsClient.getDefaultClient(testContext).listMembersFromGroup(accountId, groupName);
            boolean memberPresent = groupMembers.stream().anyMatch(memberCrn -> groupMember.getCrn().equals(memberCrn));
            LOGGER.info("Member is present '{}' at group '{}', group members: [{}]", memberPresent, groupName, groupMembers);
            if (expectedPresence) {
                if (memberPresent) {
                    LOGGER.info("User '{}' have been assigned successfully to group {}.", groupMember.getDisplayName(), groupName);
                    Log.then(LOGGER, format(" User '%s' have been assigned successfully to group '%s'. ", groupMember.getDisplayName(), groupName));
                } else {
                    throw new TestFailException(format(" User '%s' is missing from group '%s' members! ", groupMember.getDisplayName(), groupName));
                }
            } else {
                if (!memberPresent) {
                    LOGGER.info("User '{}' have been removed successfully from group {}.", groupMember.getDisplayName(), groupName);
                    Log.then(LOGGER, format(" User '%s' have been removed successfully from group '%s'. ", groupMember.getDisplayName(), groupName));
                } else {
                    throw new TestFailException(format(" User '%s' is still member of group '%s'! ", groupMember.getDisplayName(), groupName));
                }
            }
            return umsGroupTestDto;
        };
    }

    public static Assertion<UmsGroupTestDto, UmsClient> validateUserGroupPresence(String groupName, boolean expectedPresence) {
        return (testContext, umsGroupTestDto, umsClient) -> {
            String accountId = testContext.getActingUserCrn().getAccountId();
            umsGroupTestDto.withName(groupName);
            try {
                Group foundGroup = umsClient.getDefaultClient(testContext).listGroups(accountId, List.of(groupName))
                        .stream()
                        .filter(group -> StringUtils.equalsIgnoreCase(group.getGroupName(), groupName))
                        .findFirst()
                        .orElse(null);
                umsGroupTestDto.setResponse(foundGroup);
                if (expectedPresence) {
                    LOGGER.info(format(" User group '%s' is present at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s ", groupName, accountId,
                            umsGroupTestDto.getResponse().getGroupId(), umsGroupTestDto.getResponse().getCrn()));
                    Log.then(LOGGER, format(" User group '%s' is present at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s ", groupName, accountId,
                            umsGroupTestDto.getResponse().getGroupId(), umsGroupTestDto.getResponse().getCrn()));
                } else {
                    throw new TestFailException(format(" User group '%s' is still present at account '%s'! ", groupName, accountId));
                }
            } catch (StatusRuntimeException e) {
                if (expectedPresence) {
                    throw new TestFailException(format(" User group '%s' cannot be found at account '%s', because of: %s", groupName, accountId, e));
                } else {
                    LOGGER.info("User group '{}' has been removed successfully from account {}.", groupName, accountId);
                    Log.then(LOGGER, format(" User group '%s' has been removed successfully from account '%s'. ", groupName, accountId));
                }
            }
            return umsGroupTestDto;
        };
    }
}
