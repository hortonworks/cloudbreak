package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.USER_DATA_CCMV2_SETUP;

import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class UserDataCcmV2SetupPatchService extends ExistingStackPatchService {
    public static final int MAX_START_DELAY_IN_HOURS = 6;

    public static final String CDP_API_ENDPOINT_URL_ENV_VARIABLE_NAME = "CDP_API_ENDPOINT_URL";

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataCcmV2SetupPatchService.class);

    @Inject
    private UserDataService userDataService;

    @Override
    public StackPatchType getStackPatchType() {
        return USER_DATA_CCMV2_SETUP;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return stack.getTunnel().useCcmV2() && gatewayUserDataDoesNotContainCdpApiEndpointUrlVar(stack);
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        Long stackId = stack.getId();
        Map<InstanceGroupType, String> userData = userDataService.getUserData(stackId);
        String gatewayUserData = userData.get(InstanceGroupType.GATEWAY);
        LOGGER.info("Updating gateway user data for stack('{}') with exporting additional '{}' environment variable.", stackId,
                CDP_API_ENDPOINT_URL_ENV_VARIABLE_NAME);
        String updatedGatewayUserData = new UserDataReplacer(gatewayUserData)
                .replaceQuoted(CDP_API_ENDPOINT_URL_ENV_VARIABLE_NAME, "")
                .getUserData();
        userData.put(InstanceGroupType.GATEWAY, updatedGatewayUserData);
        userDataService.createOrUpdateUserData(stackId, userData);
        return true;
    }

    @Override
    public Date getFirstStart() {
        return randomDelayWithMaxHours(MAX_START_DELAY_IN_HOURS);
    }

    @Override
    protected boolean shouldCheckForFailedRetryableFlow() {
        return Boolean.FALSE;
    }

    private boolean gatewayUserDataDoesNotContainCdpApiEndpointUrlVar(Stack stack) {
        Map<InstanceGroupType, String> userData = userDataService.getUserData(stack.getId());
        return !userData.get(InstanceGroupType.GATEWAY).contains(CDP_API_ENDPOINT_URL_ENV_VARIABLE_NAME);
    }
}
