package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersInGroupV1Request;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaFindUsersInGroupAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFindUsersInGroupAction.class);

    private final Set<String> users;

    private final String group;

    private final boolean expectedPresence;

    public FreeIpaFindUsersInGroupAction(Set<String> users, String group, boolean expectedPresence) {
        this.users = users;
        this.group = group;
        this.expectedPresence = expectedPresence;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        CheckUsersInGroupV1Request checkUsersInGroupRequest = new CheckUsersInGroupV1Request();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        checkUsersInGroupRequest.setEnvironmentCrn(environmentCrn);
        checkUsersInGroupRequest.setGroup(group);
        checkUsersInGroupRequest.setUsers(users);
        Log.when(LOGGER, format(" Checking users [%s] are present in group '%s' at environment '%s'", users, group,
                environmentCrn));
        Log.whenJson(LOGGER, format(" FreeIpa '%s' find users in group request:%n ", testDto.getResponse().getCrn()), checkUsersInGroupRequest);
        if (expectedPresence) {
            if (!client.getDefaultClient().getClientTestV1Endpoint().checkUsersInGroup(checkUsersInGroupRequest).getResult()) {
                throw new TestFailException("Given freeipa users cannot be found in the given group, please check FMS logs for details");
            }
            LOGGER.info(format(" Users [%s] are present in group '%s' at environment '%s'", users, group, environmentCrn));
            Log.when(LOGGER, format(" Users [%s] are present in group '%s' at environment '%s'", users, group, environmentCrn));
        } else {
            if (client.getDefaultClient().getClientTestV1Endpoint().checkUsersInGroup(checkUsersInGroupRequest).getResult()) {
                throw new TestFailException("Given freeipa users have been found in the given group, please check FMS logs for details");
            }
            LOGGER.info(format(" Users [%s] have been removed successfully from group '%s' at environment '%s'", users, group,
                    environmentCrn));
            Log.when(LOGGER, format(" Users [%s] have been removed successfully from group '%s' at environment '%s'", users, group,
                    environmentCrn));
        }
        return testDto;
    }
}
