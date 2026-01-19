package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.test.model.CheckGroupsV1Request;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaFindGroupsAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaFindGroupsAction.class);

    private final Set<String> groups;

    public FreeIpaFindGroupsAction(Set<String> groups) {
        this.groups = groups;
    }

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        CheckGroupsV1Request checkGroupsRequest = new CheckGroupsV1Request();
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        checkGroupsRequest.setEnvironmentCrn(environmentCrn);
        checkGroupsRequest.setGroups(groups);
        Log.when(LOGGER, format(" Checking groups [%s] are present at environment '%s'", groups, environmentCrn));
        Log.whenJson(LOGGER, format(" FreeIpa '%s' find groups request:%n ", testDto.getResponse().getCrn()), checkGroupsRequest);
        if (!client.getDefaultClient(testContext).getClientTestV1Endpoint().checkGroups(checkGroupsRequest).getResult()) {
            throw new TestFailException("Given freeipa groups cannot be found, please check FMS logs for details");
        }
        LOGGER.info(format(" Groups [%s] are present at environment '%s'", groups, environmentCrn));
        Log.when(LOGGER, format(" Groups [%s] are present at environment '%s'", groups, environmentCrn));
        return testDto;
    }
}
