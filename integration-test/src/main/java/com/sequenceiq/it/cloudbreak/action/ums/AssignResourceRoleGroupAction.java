package com.sequenceiq.it.cloudbreak.action.ums;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class AssignResourceRoleGroupAction extends AbstractUmsAction<UmsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignResourceRoleGroupAction.class);

    private final String groupCrn;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public AssignResourceRoleGroupAction(String groupCrn, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.groupCrn = groupCrn;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    protected UmsTestDto umsAction(TestContext testContext, UmsTestDto testDto, UmsClient client) throws Exception {
        String resourceRole = testDto.getRequest().getRoleCrn();
        String resourceCrn = testDto.getRequest().getResourceCrn();
        Log.when(LOGGER, format(" Assigning resource role '%s' at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        Log.whenJson(LOGGER, format(" Assign resource role request:%n "), testDto.getRequest());
        client.getDefaultClient().assignResourceRole(groupCrn, resourceCrn, resourceRole, regionAwareInternalCrnGeneratorFactory);
        // wait for UmsRightsCache to expire
        Thread.sleep(7000);
        LOGGER.info(format(" Resource role '%s' has been assigned at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        Log.when(LOGGER, format(" Resource role '%s' has been assigned at resource '%s' for group '%s' ", resourceRole, resourceCrn, groupCrn));
        return testDto;
    }
}
