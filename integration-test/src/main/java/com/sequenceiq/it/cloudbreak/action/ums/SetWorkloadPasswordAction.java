package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class SetWorkloadPasswordAction implements Action<UmsTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetWorkloadPasswordAction.class);

    private final String newPassword;

    public SetWorkloadPasswordAction(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        String userCrn = testContext.getActingUserCrn().toString();
        String workloadUsername = client.getDefaultClient().getUserDetails(userCrn, userCrn, Optional.of("")).getWorkloadUsername();
        LOGGER.info("Setting new workload password '{}' for user '{}' with workload username '{}'", newPassword, userCrn, workloadUsername);
        Log.when(LOGGER, format(" Setting new workload password '%s' for user '%s' workload username '%s' ", newPassword, userCrn, workloadUsername));
        client.getDefaultClient().setActorWorkloadPassword(userCrn, userCrn, newPassword, Optional.of(""));
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        LOGGER.info("New workload password has been set for '{}' with workload username '{}'!", userCrn, workloadUsername);
        Log.when(LOGGER, format(" New workload password has been set for '%s' with workload username '%s'! ", userCrn, workloadUsername));
        return testDto;
    }
}
