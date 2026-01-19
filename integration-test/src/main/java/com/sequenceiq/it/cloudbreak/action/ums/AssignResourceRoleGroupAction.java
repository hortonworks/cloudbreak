package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;

public class AssignResourceRoleGroupAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignResourceRoleGroupAction.class);

    private final String groupCrn;

    public AssignResourceRoleGroupAction(String groupCrn) {
        this.groupCrn = groupCrn;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        String resourceCrn = testDto.getRequest().getResourceCrn();
        String resourceRole = UmsClientUtils.getResourceRoleCrn(testDto, client);
        Log.when(LOGGER, format(" Assigning resource role '%s' at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        Log.whenJson(LOGGER, format(" Assign resource role request:%n "), testDto.getRequest());

        client.getDefaultClient(testContext).assignResourceRole(groupCrn, resourceCrn, resourceRole);
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        LOGGER.info(format(" Resource role '%s' has been assigned at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        Log.when(LOGGER, format(" Resource role '%s' has been assigned at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        return testDto;
    }
}
