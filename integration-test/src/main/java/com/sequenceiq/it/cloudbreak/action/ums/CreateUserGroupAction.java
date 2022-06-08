package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class CreateUserGroupAction implements Action<UmsGroupTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserGroupAction.class);

    private final String groupName;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CreateUserGroupAction(String groupName, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.groupName = groupName;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    public UmsGroupTestDto action(TestContext testContext, UmsGroupTestDto testDto, UmsClient client) throws Exception {
        String accountId = testDto.getRequest().getAccountId();
        testDto.withName(groupName);
        Log.when(LOGGER, format(" Creating new user group '%s' at account '%s'. ", groupName, accountId));
        Log.whenJson(LOGGER, format(" Create new user group request:%n "), testDto.getRequest());
        testDto.setResponse(client.getDefaultClient().createGroup(accountId, groupName, regionAwareInternalCrnGeneratorFactory));
        LOGGER.info(format(" New user group has been created at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s %nGroup Name: %s. ",
                accountId, testDto.getResponse().getGroupId(), testDto.getResponse().getCrn(), testDto.getResponse().getGroupName()));
        Log.when(LOGGER, format(" New user group has been created at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s %nGroup Name: %s. ",
                accountId, testDto.getResponse().getGroupId(), testDto.getResponse().getCrn(), testDto.getResponse().getGroupName()));
        return testDto;
    }
}
