package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class UserDataMigrationPatchService extends ExistingStackPatchService {

    @Inject
    private UserDataService userDataService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.USER_DATA_MIGRATION;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return stack.getTunnel().useCcmV2Jumpgate() && !isJumpgateEnabledInGwUserdata(stack);
    }

    private boolean isJumpgateEnabledInGwUserdata(Stack stack) {
        Map<InstanceGroupType, String> userData = userDataService.getUserData(stack.getId());
        return userData.get(InstanceGroupType.GATEWAY).contains("IS_CCM_V2_JUMPGATE_ENABLED=true");
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        userDataService.updateJumpgateFlagOnly(stack.getId());
        return true;
    }
}
