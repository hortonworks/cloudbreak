package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ListGroupMembersAction implements Action<UmsGroupTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListGroupMembersAction.class);

    private final String groupName;

    public ListGroupMembersAction(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public UmsGroupTestDto action(TestContext testContext, UmsGroupTestDto testDto, UmsClient client) throws Exception {

        String userCrn = testContext.getActingUserCrn().toString();
        String accountId = testDto.getRequest().getAccountId();
        testDto.withName(groupName);
        Log.when(LOGGER, format(" Listing user group '%s' members at account '%s'. ", groupName, accountId));
        Log.whenJson(LOGGER, format(" List user group members request:%n "), testDto.getRequest());
        List<String> members = client.getDefaultClient().listMembersFromGroup(userCrn, accountId, groupName, Optional.of(""));
        LOGGER.info(format(" User group '%s' contains members: [%s] ", groupName, members));
        Log.when(LOGGER, format(" User group '%s' contains members: [%s] ", groupName, members));
        return testDto;
    }
}
