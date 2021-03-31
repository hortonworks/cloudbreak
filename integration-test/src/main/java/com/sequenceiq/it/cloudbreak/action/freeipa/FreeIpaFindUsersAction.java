package com.sequenceiq.it.cloudbreak.action.freeipa;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckUsersV1Request;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class FreeIpaFindUsersAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFindUsersAction.class);

    private final Set<String> users;

    public FreeIpaFindUsersAction(Set<String> users) {
        this.users = users;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        CheckUsersV1Request checkUsersRequest = new CheckUsersV1Request();
        checkUsersRequest.setEnvironmentCrn(testDto.getResponse().getEnvironmentCrn());
        checkUsersRequest.setUsers(users);
        if (!client.getDefaultClient().getClientTestV1Endpoint().checkUsers(checkUsersRequest).getResult()) {
            throw new TestFailException("Given freeipa users cannot be found, please check FMS logs for details");
        }
        return testDto;
    }
}
