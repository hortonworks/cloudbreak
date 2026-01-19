package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

public class DeleteUserGroupAction implements Action<UmsGroupTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUserGroupAction.class);

    private final String groupName;

    public DeleteUserGroupAction(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public UmsGroupTestDto action(TestContext testContext, UmsGroupTestDto testDto, UmsClient client) throws Exception {
        String accountId = testDto.getRequest().getAccountId();
        testDto.withName(groupName);
        Log.when(LOGGER, format(" Deleting user group '%s' at account '%s'. ", groupName, accountId));
        Log.whenJson(LOGGER, format(" Delete user group request:%n "), testDto.getRequest());
        client.getDefaultClient(testContext).deleteGroup(accountId, groupName);
        LOGGER.info(format(" User group '%s' has been deleted at account '%s'. ", groupName, accountId));
        Log.when(LOGGER, format(" User group '%s' has been deleted at account '%s'. ", groupName, accountId));
        return testDto;
    }
}
