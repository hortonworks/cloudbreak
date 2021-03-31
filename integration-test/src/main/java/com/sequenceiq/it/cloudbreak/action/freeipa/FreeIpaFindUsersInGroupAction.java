package com.sequenceiq.it.cloudbreak.action.freeipa;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersInGroupV1Request;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class FreeIpaFindUsersInGroupAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFindUsersInGroupAction.class);

    private final Set<String> users;

    private final String group;

    public FreeIpaFindUsersInGroupAction(Set<String> users, String group) {
        this.users = users;
        this.group = group;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        CheckUsersInGroupV1Request checkUsersInGroupRequest = new CheckUsersInGroupV1Request();
        checkUsersInGroupRequest.setEnvironmentCrn(testDto.getResponse().getEnvironmentCrn());
        checkUsersInGroupRequest.setGroup(group);
        checkUsersInGroupRequest.setUsers(users);
        if (!client.getDefaultClient().getClientTestV1Endpoint().checkUsersInGroup(checkUsersInGroupRequest).getResult()) {
            throw new TestFailException("Given freeipa users cannot be found in the given group, please check FMS logs for details");
        }
        return testDto;
    }
}
