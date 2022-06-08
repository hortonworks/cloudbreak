package com.sequenceiq.it.cloudbreak.assertion.ums;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class UserGroupTestAssertion {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserGroupTestAssertion.class);

    private UserGroupTestAssertion() {
    }

    public static Assertion<UmsGroupTestDto, UmsClient> validateUserGroupMembership(CloudbreakUser groupMember, String groupName, boolean expectedPresence,
            RegionAwareInternalCrnGeneratorFactory crnGeneratorFactory) {
        return (testContext, umsGroupTestDto, umsClient) -> {
            String accountId = testContext.getActingUserCrn().getAccountId();

            List<String> groupMembers = umsClient.getDefaultClient().listMembersFromGroup(accountId, groupName, crnGeneratorFactory);
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
}
