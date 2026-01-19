package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersV1Request;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaFindUsersAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFindUsersAction.class);

    private final Set<String> users;

    private final boolean expectedPresence;

    public FreeIpaFindUsersAction(Set<String> users, boolean expectedPresence) {
        this.users = users;
        this.expectedPresence = expectedPresence;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        CheckUsersV1Request checkUsersRequest = new CheckUsersV1Request();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        checkUsersRequest.setEnvironmentCrn(environmentCrn);
        checkUsersRequest.setUsers(users);
        Log.when(LOGGER, format(" Checking users [%s] are present at environment '%s'", users, environmentCrn));
        Log.whenJson(LOGGER, format(" FreeIpa '%s' find users request:%n ", testDto.getResponse().getCrn()), checkUsersRequest);
        if (expectedPresence) {
            if (!client.getDefaultClient(testContext).getClientTestV1Endpoint().checkUsers(checkUsersRequest).getResult()) {
                throw new TestFailException("Given freeipa users cannot be found, please check FMS logs for details!");
            }
            LOGGER.info(format(" Users [%s] are present at environment '%s'", users, environmentCrn));
            Log.when(LOGGER, format(" Users [%s] are present at environment '%s'", users, environmentCrn));
        } else {
            if (client.getDefaultClient(testContext).getClientTestV1Endpoint().checkUsers(checkUsersRequest).getResult()) {
                throw new TestFailException("Given freeipa users have been found, please check FMS logs for details!");
            }
            LOGGER.info(format(" Users [%s] have been removed successfully from environment '%s'", users, environmentCrn));
            Log.when(LOGGER, format(" Users [%s] have been removed successfully from environment '%s'", users, environmentCrn));
        }
        return testDto;
    }
}
