package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Group;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsGroupTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

import io.grpc.StatusRuntimeException;

public class CreateUserGroupAction implements Action<UmsGroupTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserGroupAction.class);

    private final String groupName;

    public CreateUserGroupAction(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public UmsGroupTestDto action(TestContext testContext, UmsGroupTestDto testDto, UmsClient client) throws Exception {
        String accountId = testDto.getRequest().getAccountId();
        testDto.withName(groupName);
        try {
            Group existingGroup = client.getDefaultClient(testContext).listGroups(accountId, List.of(groupName))
                    .stream()
                    .filter(group -> StringUtils.equalsIgnoreCase(group.getGroupName(), groupName))
                    .findFirst()
                    .orElse(null);
            List<String> groupMembers = client.getDefaultClient(testContext).listMembersFromGroup(accountId, groupName);
            testDto.setResponse(existingGroup);
            LOGGER.info(format(" User group is already present at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s %nGroup Name: %s " +
                            "%nGroup Members: %s. So creation is not necessary! ", accountId, testDto.getResponse().getGroupId(),
                    testDto.getResponse().getCrn(), testDto.getResponse().getGroupName(), groupMembers));
            Log.when(LOGGER, format(" User group is already present at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s %nGroup Name: %s " +
                            "%nGroup Members: %s. So creation is not necessary! ", accountId, testDto.getResponse().getGroupId(),
                    testDto.getResponse().getCrn(), testDto.getResponse().getGroupName(), groupMembers));
        } catch (StatusRuntimeException e) {
            Log.when(LOGGER, format(" Creating new user group '%s' at account '%s'. ", groupName, accountId));
            Log.whenJson(LOGGER, format(" Create new user group request:%n "), testDto.getRequest());
            testDto.setResponse(client.getDefaultClient(testContext).createGroup(accountId, groupName));
            LOGGER.info(format(" New user group has been created at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s %nGroup Name: %s. ",
                    accountId, testDto.getResponse().getGroupId(), testDto.getResponse().getCrn(), testDto.getResponse().getGroupName()));
            Log.when(LOGGER, format(" New user group has been created at account '%s' with details: %nGroup Id: %s %nGroup Crn: %s %nGroup Name: %s. ",
                    accountId, testDto.getResponse().getGroupId(), testDto.getResponse().getCrn(), testDto.getResponse().getGroupName()));
        }
        return testDto;
    }
}
