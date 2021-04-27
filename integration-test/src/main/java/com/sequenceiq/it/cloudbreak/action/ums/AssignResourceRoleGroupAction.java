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

public class AssignResourceRoleGroupAction implements Action<UmsTestDto, UmsClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignResourceRoleGroupAction.class);

    private final String groupCrn;

    public AssignResourceRoleGroupAction(String groupCrn) {
        this.groupCrn = groupCrn;
    }

    @Override
    public UmsTestDto action(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        String resourceRole = testDto.getRequest().getRoleCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();
        Log.when(LOGGER, format(" Assigning resource role '%s' at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        Log.whenJson(LOGGER, format(" Assign resource role request:%n "), testDto.getRequest());
        client.getDefaultClient().assignResourceRole(groupCrn, resourceCrn, resourceRole, Optional.of(""));
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        LOGGER.info(format(" Resource role '%s' has been assigned at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        Log.when(LOGGER, format(" Resource role '%s' has been assigned at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        return testDto;
    }
}
